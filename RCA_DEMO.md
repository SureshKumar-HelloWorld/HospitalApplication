# Root Cause Analysis (RCA) Demo & Observability Guide

## Overview
This document demonstrates how to use the Health Management Platform's observability stack to identify, trace, and resolve issues through structured logging, distributed tracing, and metrics collection.

## Architecture Components

### Observability Stack
- **Logs**: Structured JSON logging via Logback + Logstash encoder
- **Traces**: OpenTelemetry SDK with Jaeger backend (OTLP protocol)
- **Metrics**: Micrometer + Prometheus registry
- **Correlation**: X-Correlation-ID header propagation for request tracing

### Services
- `user-service` (port 8081): Authentication & user management
- `patient-service` (port 8082): Patient profiles & auth
- `appointment-service` (port 8083): Appointment booking with Kafka events

### Backend Infrastructure
- **Database**: MySQL (auto-created databases: userdb, patientdb, appointmentdb)
- **Message Queue**: Kafka for appointment events
- **Tracing Backend**: Jaeger (listening on http://localhost:16686)
- **Metrics**: Prometheus (listening on http://localhost:9090)
- **Logs**: Centralized via Docker/Kubernetes log aggregation

---

## RCA Scenario 1: Patient Registration Failure

### Symptoms
Frontend user attempts to register a new patient account but receives HTTP 500 error.

### Root Cause Investigation Steps

#### Step 1: Check Structured Logs
```bash
# View patient-service logs (tail the most recent)
docker logs -f patient-service 2>&1 | grep -E "ERROR|WARN|register"

# Or in Kubernetes:
kubectl logs -l app=patient-service -n health --tail=100 | grep -E "ERROR|WARN"

# Expected output showing the failure:
{
  "timestamp": "2026-06-01T14:32:10Z",
  "level": "ERROR",
  "message": "Patient registration failed",
  "service": "patient-service",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "exception": "org.springframework.dao.DataIntegrityViolationException: Duplicate entry"
}
```

#### Step 2: Trace the Request in Jaeger
1. Open Jaeger UI: http://localhost:16686
2. Select "patient-service" from service dropdown
3. Look for spans with operation name "patient.register"
4. Click on the failed trace to view timeline:
   - `POST /api/auth/register` (HTTP handler)
   - `patient.register` (service span)
   - `repository.save` (database operation)
   - `patient.register.exception` (error recorded)

Expected trace shows:
- Email validation check ✓
- Password encoding ✓
- Database insert ✗ (Duplicate email constraint violation)

#### Step 3: Correlate Logs and Traces
The `correlationId` from logs matches the trace ID in Jaeger, allowing you to:
- Find all service calls in the request chain
- Identify where the failure occurred
- See timing information (which operation was slowest)

#### Step 4: Root Cause Confirmation
In this scenario:
- **Root Cause**: Email already exists in database
- **Evidence**: Database constraint violation exception in trace
- **Logs show**: "Registration failed: Email already registered"
- **Resolution**: Ask user to use a different email or reset password

---

## RCA Scenario 2: Slow Appointment Booking

### Symptoms
Appointment booking takes >5 seconds, but the frontend shows no error.

### Root Cause Investigation Steps

#### Step 1: Check Metrics in Prometheus
1. Open Prometheus: http://localhost:9090
2. Query: `rate(http_requests_total{service="appointment-service"}[5m])`
3. Look for latency metrics: `http_request_duration_seconds_bucket`
4. Filter by endpoint: `/api/appointments`

Expected graph shows:
- Request count: 2 req/min
- P99 latency: 5+ seconds
- P50 latency: 1-2 seconds

#### Step 2: Analyze Traces for Performance Bottleneck
1. Jaeger UI → appointment-service → `appointment.book` span
2. View the trace timeline to see which operation is slow:
   - `doctor.fetch` (100ms) ✓
   - `availability.check` (500ms) ✓
   - `kafka.publish` (4000ms) ✗ (the bottleneck)
   - `database.save` (300ms) ✓

#### Step 3: Check Kafka Connectivity
```bash
# View Kafka container logs
docker logs -f kafka 2>&1 | tail -50

# Or check service logs for Kafka errors:
docker logs appointment-service | grep -i "kafka"

# Expected: Connection refused or broker unavailable errors
```

#### Step 4: Verify Correlation ID Propagation
In the Jaeger trace, expand the `kafka.publish` span:
- Tags show: `messaging.system: kafka`, `messaging.destination: appointments`
- The same `correlationId` is propagated to Kafka headers
- This allows tracking the message in downstream consumer services

#### Step 5: Root Cause Confirmation
- **Root Cause**: Kafka broker is unavailable or slow
- **Evidence**: `kafka.publish` span shows 4000ms latency
- **Logs show**: Connection timeout or broker errors
- **Resolution**: Restart Kafka, check network connectivity, scale Kafka cluster

---

## RCA Scenario 3: Failed Cross-Service Authentication

### Symptoms
Login works, but fetching patient data returns 401 Unauthorized.

### Root Cause Investigation Steps

#### Step 1: Examine JWT Token in Traces
1. Jaeger UI → patient-service → `patient.getProfile` span
2. Check span attributes for JWT-related information
3. Look for security-related events in the trace

#### Step 2: Check Service Logs for JWT Errors
```bash
# View patient-service security logs
docker logs patient-service | grep -E "JWT|security|Unauthorized"

# Expected output:
{
  "timestamp": "2026-06-01T14:40:00Z",
  "level": "WARN",
  "message": "JWT validation failed",
  "service": "patient-service",
  "correlationId": "550e8400-e29b-41d4-a716-446655440001",
  "jwt.error": "JWT expired"
}
```

#### Step 3: Verify Correlation ID in Request Headers
```bash
# Simulate request with curl to see full trace:
curl -v \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "X-Correlation-ID: 550e8400-e29b-41d4-a716-446655440001" \
  http://localhost:8082/api/patients

# Check response headers:
# X-Correlation-ID: 550e8400-e29b-41d4-a716-446655440001 (should be echoed back)
```

#### Step 4: Root Cause Confirmation
- **Root Cause**: JWT token is expired or invalid
- **Evidence**: JWT validation failed in logs, 401 response code
- **Solution**: User needs to re-login to get a fresh token

---

## OpenTelemetry Instrumentation in Code

### Example: Custom Span in PatientServiceImpl

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl implements PatientService {
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("patient-service");

    @Override
    @Transactional
    public PatientDto register(RegisterRequest request) {
        Span span = tracer.spanBuilder("patient.register").startSpan();
        try (var scope = span.makeCurrent()) {
            // Add context attributes for debugging
            span.setAttributes(Attributes.builder()
                    .put("patient.email", request.getEmail())
                    .put("patient.firstName", request.getFirstName())
                    .build());
            
            log.info("Attempting to register patient with email: {}", request.getEmail());
            
            if (patientRepository.existsByEmail(request.getEmail())) {
                log.warn("Registration failed: Email already registered");
                span.recordException(new RuntimeException("Email already registered"));
                throw new RuntimeException("Email already registered");
            }
            
            // Business logic...
            Patient saved = patientRepository.save(patient);
            
            // Record success event
            span.addEvent("patient.registered", Attributes.builder()
                    .put("patient.id", saved.getId())
                    .build());
            
            log.info("Patient registered successfully with id: {}", saved.getId());
            return toDto(saved);
        } catch (Exception ex) {
            span.recordException(ex);
            log.error("Patient registration failed", ex);
            throw ex;
        } finally {
            span.end();
        }
    }
}
```

### Key Instrumentation Patterns

1. **Span Creation**: `tracer.spanBuilder(operationName).startSpan()`
2. **Scope Management**: `try (var scope = span.makeCurrent())`
3. **Attributes**: `span.setAttributes(Attributes.builder().put(...).build())`
4. **Events**: `span.addEvent("eventName", attributes)`
5. **Exceptions**: `span.recordException(exception)`
6. **Logging**: Structured logs with `correlationId` in MDC

---

## Structured Logging Format

All services produce JSON-structured logs with the following fields:

```json
{
  "timestamp": "2026-06-01T14:32:10Z",
  "level": "INFO",
  "logger": "com.health.patient.service.PatientServiceImpl",
  "message": "Patient registered successfully",
  "service": "patient-service",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "9d2149eb8dcfb0e4cd1b2b1e1e8b1e2c",
  "spanId": "9d2149eb8dcfb0e4",
  "patient.id": 123,
  "patient.email": "john@example.com",
  "thread": "http-nio-8082-exec-1"
}
```

**Key Fields**:
- `correlationId`: Unique request ID for cross-service tracing
- `traceId`: OpenTelemetry distributed trace ID
- `spanId`: Individual span within the trace
- Custom fields (e.g., `patient.id`, `patient.email`) for context

---

## Debugging Checklist

### For API Failures
- [ ] Check service logs for ERROR/WARN level entries
- [ ] Find `correlationId` in the error log
- [ ] Open Jaeger and search for that `traceId`
- [ ] Examine span timeline to find exact failure point
- [ ] Check upstream services' logs for cascading failures
- [ ] Verify database connectivity (check MySQL logs)

### For Performance Issues
- [ ] Check Prometheus metrics for request latency
- [ ] Open Jaeger trace and identify slowest span
- [ ] Check service logs for timeout or resource warnings
- [ ] Verify Kafka/database is running and healthy
- [ ] Check for database slow query logs
- [ ] Review span count (many spans = high overhead)

### For Authorization/Security Issues
- [ ] Check JWT logs for expiration or validation errors
- [ ] Verify CORS configuration if frontend fails
- [ ] Check `X-Correlation-ID` header propagation
- [ ] Review SecurityConfig for endpoint permissions
- [ ] Check user/role assignments in database

---

## Quick Commands

### View Logs (Docker)
```bash
# Follow patient-service logs
docker logs -f patient-service

# Filter for errors
docker logs patient-service | grep ERROR

# Last N lines
docker logs patient-service --tail=50
```

### View Logs (Kubernetes)
```bash
# Follow pod logs
kubectl logs -f pod/patient-service-xyz -n health

# View all service logs
kubectl logs -l app=patient-service -n health --tail=100

# Export logs for analysis
kubectl logs -l app=patient-service -n health > patient-logs.txt
```

### Query Prometheus
```bash
# Request count per second
rate(http_requests_total[1m])

# Request latency (P95)
histogram_quantile(0.95, http_request_duration_seconds_bucket)

# Service health
up{service="patient-service"}
```

### Trace Correlation
```bash
# Using correlationId to find all related logs
docker logs patient-service | grep "550e8400-e29b-41d4-a716-446655440000"

# Find in Jaeger UI: Search by Trace ID or Tags
# Tags: correlationId=550e8400-e29b-41d4-a716-446655440000
```

---

## Best Practices for RCA

1. **Always Include Correlation ID**: Every request should have `X-Correlation-ID` header
2. **Log at Appropriate Levels**:
   - `INFO`: Successful operations, state changes
   - `WARN`: Recoverable errors, unusual conditions
   - `ERROR`: Failures requiring investigation
   - `DEBUG`: Detailed diagnostic info
3. **Use Structured Logging**: Include context fields (IDs, values) in logs
4. **Add Spans for Critical Operations**: Registration, login, data mutations
5. **Record Exceptions**: Always capture exceptions in spans for later analysis
6. **Set Attributes**: Add relevant business context to spans
7. **Use Events**: Mark important milestones in span timeline

---

## Testing the Observability Stack

### End-to-End Test
```bash
# 1. Register a patient (check logs, traces, metrics)
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-123" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com",
    "password": "password123",
    "phoneNumber": "555-1234"
  }'

# 2. Check JSON logs
docker logs patient-service | grep "test@example.com" | tail -1

# 3. Find trace in Jaeger
# - Open http://localhost:16686
# - Service: patient-service
# - Search tag: correlationId=test-123

# 4. Check metrics in Prometheus
# - Query: http_requests_total{service="patient-service"}
# - Verify registration request count increased
```

---

## Troubleshooting the Observability Stack

### Jaeger not showing traces
- Verify OTLP exporter endpoint: `http://localhost:4317`
- Check service logs for export errors
- Ensure OpenTelemetry SDK is initialized
- Verify Jaeger collector is running

### Prometheus not scraping metrics
- Check Prometheus scrape config for service targets
- Verify actuator endpoints are exposed: `/actuator/prometheus`
- Check service is returning metrics in Prometheus format

### Logs not appearing in aggregation
- Verify Logback configuration includes JSON encoder
- Check `correlationId` is in MDC
- Ensure log level is not suppressing messages

---

## Summary
This observability stack enables rapid RCA by providing three complementary views:
1. **Logs**: Immediate human-readable context
2. **Traces**: Request flow and timing across services
3. **Metrics**: System health and performance trends

Use `correlationId` as the bridge to connect all three views and quickly identify root causes.
