# Developer Guide: Health Insurance Management Platform

## Project Overview

This is an enterprise-grade Healthcare Management Platform built with:
- **Backend**: Java 17, Spring Boot 3.1.4, Spring Cloud microservices
- **Frontend**: React 18.2.0 with TypeScript, Vite build tool
- **Database**: MySQL 8.0.33 with Flyway migrations
- **Message Queue**: Kafka for asynchronous events
- **Observability**: OpenTelemetry + Jaeger (distributed tracing), Prometheus (metrics), structured JSON logging
- **Security**: JWT authentication with Spring Security, BCryptPasswordEncoder
- **Deployment**: Docker containers, Kubernetes manifests

---

## Quick Start

### Prerequisites
- Java 17
- Maven 3.8+
- Node.js 16+
- Docker & Docker Compose
- Git

### 1. Clone and Build

```bash
# Navigate to project directory
cd /d/HealthInsuranceApplication

# Build all microservices
mvn clean package -DskipTests

# Build frontend
cd frontend
npm install
npm run build
```

### 2. Start Infrastructure (Docker Compose)

```bash
# From project root, start MySQL, Kafka, Jaeger, Prometheus
docker-compose up -d

# Verify all services are running
docker ps
```

### 3. Run Services (Development Mode)

**Terminal 1: user-service**
```bash
cd user-service
mvn spring-boot:run
# Listening on http://localhost:8081
```

**Terminal 2: patient-service**
```bash
cd patient-service
mvn spring-boot:run
# Listening on http://localhost:8082
```

**Terminal 3: appointment-service**
```bash
cd appointment-service
mvn spring-boot:run
# Listening on http://localhost:8083
```

**Terminal 4: frontend**
```bash
cd frontend
npm run dev
# Listening on http://localhost:5173
```

### 4. Verify All Services

```bash
# Check service health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health

# Access frontend
open http://localhost:5173

# Check observability
# Jaeger: http://localhost:16686
# Prometheus: http://localhost:9090
```

---

## Architecture

### Microservices

#### User Service (Port 8081)
**Purpose**: User authentication and JWT token generation
- Endpoint: `POST /api/auth/login` - User login
- Endpoint: `POST /api/auth/register` - User registration
- Database: `userdb`
- Dependencies: Spring Security, JWT, BCryptPasswordEncoder

#### Patient Service (Port 8082)
**Purpose**: Patient profile management and CRUD operations
- Endpoint: `POST /api/auth/register` - Patient registration
- Endpoint: `POST /api/auth/login` - Patient login
- Endpoint: `GET /api/patients` - List all patients
- Endpoint: `GET /api/patients/{id}` - Get patient details
- Endpoint: `PUT /api/patients/{id}` - Update patient
- Endpoint: `DELETE /api/patients/{id}` - Delete patient
- Database: `patientdb`
- Dependencies: BCryptPasswordEncoder, JWT

#### Appointment Service (Port 8083)
**Purpose**: Doctor management and appointment booking with conflict detection
- Endpoint: `GET /api/doctors` - List all doctors
- Endpoint: `GET /api/appointments` - List all appointments
- Endpoint: `POST /api/appointments` - Book appointment
- Endpoint: `DELETE /api/appointments/{id}` - Cancel appointment
- Database: `appointmentdb`
- Dependencies: Kafka (appointment events), Repository pattern
- Conflict Detection: Checks `countByDoctorAndDateTimeAndStatus == 0` before booking

#### Frontend (Port 5173)
**Purpose**: Single-page application for user interaction
- Technology: React, TypeScript, Vite, Axios
- API Clients:
  - `authApi`: http://localhost:8082 (patient auth)
  - `patientApi`: http://localhost:8082 (patient data)
  - `appointmentApi`: http://localhost:8083 (appointments)
- Features:
  - User registration & login
  - Patient browsing
  - Appointment booking
  - Dark theme UI

---

## Development Workflow

### Adding a New Endpoint

1. **Define DTO** (Data Transfer Object)
   ```java
   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   public class MyRequest {
       private String name;
       private Long id;
   }
   ```

2. **Create Service Method** with OpenTelemetry instrumentation
   ```java
   @Transactional
   public MyDto processRequest(MyRequest request) {
       Span span = tracer.spanBuilder("service.operation").startSpan();
       try (var scope = span.makeCurrent()) {
           log.info("Processing request: {}", request);
           // Business logic
           span.addEvent("operation.complete");
           return result;
       } catch (Exception ex) {
           span.recordException(ex);
           throw ex;
       } finally {
           span.end();
       }
   }
   ```

3. **Create Controller**
   ```java
   @RestController
   @RequestMapping("/api/resource")
   @RequiredArgsConstructor
   public class MyController {
       private final MyService service;
       
       @PostMapping
       public ResponseEntity<MyDto> create(@RequestBody MyRequest request) {
           MyDto result = service.processRequest(request);
           return ResponseEntity.ok(result);
       }
   }
   ```

4. **Add Database Migration** (Flyway)
   Create file: `src/main/resources/db/migration/V1__initial.sql`
   ```sql
   CREATE TABLE IF NOT EXISTS my_table (
       id BIGINT PRIMARY KEY AUTO_INCREMENT,
       name VARCHAR(255) NOT NULL,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   ```

5. **Create Unit Tests**
   ```java
   @ExtendWith(MockitoExtension.class)
   class MyServiceTest {
       @Mock
       private MyRepository repository;
       @InjectMocks
       private MyService service;
       
       @Test
       void testProcessRequest() {
           MyRequest request = new MyRequest("test", 1L);
           // Setup mocks
           when(repository.save(any())).thenReturn(entity);
           // Execute
           MyDto result = service.processRequest(request);
           // Verify
           assertNotNull(result);
           verify(repository, times(1)).save(any());
       }
   }
   ```

### Running Tests

```bash
# Run all tests in a service
cd patient-service
mvn test

# Run specific test class
mvn test -Dtest=PatientServiceImplTest

# Run with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Code Quality

```bash
# Check with Maven
mvn clean compile

# View compiler warnings
mvn -X clean compile 2>&1 | grep -i warning

# Check for code smells
mvn sonar:sonar -Dsonar.projectKey=health-platform -Dsonar.host.url=http://localhost:9000
```

---

## Observability & Debugging

### View Structured Logs

```bash
# Follow logs from a service
docker logs -f patient-service

# Filter for specific level
docker logs patient-service | grep ERROR

# Search for correlation ID
docker logs patient-service | grep "550e8400-e29b-41d4-a716-446655440000"

# Export logs for analysis
docker logs patient-service > patient-logs.txt 2>&1
```

### Distributed Tracing with Jaeger

1. Open http://localhost:16686
2. Select service from dropdown
3. Choose operation (e.g., `patient.register`)
4. View trace timeline showing:
   - Request flow across services
   - Span duration and relationships
   - Attributes and events
   - Exception details if any

### Metrics with Prometheus

1. Open http://localhost:9090
2. Query examples:
   ```
   # Request rate (requests per second)
   rate(http_requests_total[1m])
   
   # Request duration (P95)
   histogram_quantile(0.95, http_request_duration_seconds_bucket)
   
   # Service availability
   up{service="patient-service"}
   
   # Database connection pool
   hikaricp_connections{service="patient-service"}
   ```

### Checking Service Health

```bash
# Health endpoint
curl http://localhost:8082/actuator/health

# Metrics endpoint (Prometheus format)
curl http://localhost:8082/actuator/prometheus | grep http_requests_total

# Application info
curl http://localhost:8082/actuator/info
```

---

## Database Management

### Connect to MySQL

```bash
# From Docker
docker exec -it mysql mysql -u root -p
# Password: root_password

# List databases
SHOW DATABASES;

# View patient data
USE patientdb;
SELECT * FROM patients;

# Check migrations
SELECT * FROM flyway_schema_history;
```

### Reset Database

```bash
# Delete all data (keep schema)
docker exec mysql mysql -u root -proot_password patientdb -e "TRUNCATE TABLE patients;"

# Reset entire database (removes schema + data)
docker exec mysql mysql -u root -proot_password -e "DROP DATABASE patientdb;"
# Service will auto-create on next startup
```

---

## Deployment

### Docker Build & Run

```bash
# Build Docker image for a service
cd patient-service
docker build -t patient-service:1.0 .

# Run image
docker run -p 8082:8082 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/patientdb?createDatabaseIfNotExist=true \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  patient-service:1.0
```

### Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace health

# Deploy all services
kubectl apply -f k8s/mysql.yaml -n health
kubectl apply -f k8s/kafka.yaml -n health
kubectl apply -f k8s/jaeger.yaml -n health
kubectl apply -f k8s/prometheus.yaml -n health
kubectl apply -f k8s/user-service.yaml -n health
kubectl apply -f k8s/patient-service.yaml -n health
kubectl apply -f k8s/appointment-service.yaml -n health

# Port forwarding
kubectl port-forward svc/jaeger 16686:16686 -n health
kubectl port-forward svc/prometheus 9090:9090 -n health
kubectl port-forward svc/mysql 3306:3306 -n health

# Check pod status
kubectl get pods -n health
kubectl describe pod <pod-name> -n health
kubectl logs <pod-name> -n health
```

---

## Performance Tuning

### Database Optimization

```yaml
# Configure MySQL connection pool (application.yml)
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
```

### Caching Strategy

```java
@EnableCaching
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("patients", "doctors");
    }
}
```

### Async Processing

```java
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.initialize();
        return executor;
    }
}
```

---

## Troubleshooting

### Service Won't Start

1. Check port is not in use: `netstat -ano | grep 8082` (Windows) or `lsof -i :8082` (Mac/Linux)
2. Check database is running: `docker ps | grep mysql`
3. Check logs: `docker logs patient-service`
4. Verify JVM version: `java -version` (should be Java 17+)

### Database Connection Errors

1. Verify MySQL is accessible: `docker exec mysql mysql -u root -proot_password -e "SELECT 1;"`
2. Check JDBC URL: Should include `?createDatabaseIfNotExist=true`
3. Verify credentials in `application.yml`
4. Check firewall/networking

### Kafka/Event Issues

1. Check Kafka is running: `docker logs kafka`
2. Verify bootstrap servers configured: Check `application.yml`
3. Check event topic exists: `docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092`
4. View Kafka logs for errors

### Observability Stack Issues

1. **No traces in Jaeger**: Check OpenTelemetry exporter endpoint `http://localhost:4317`
2. **No metrics in Prometheus**: Check actuator exposed `management.endpoints.web.include=prometheus`
3. **Missing logs**: Verify Logback configuration and log level

---

## Common Commands Cheatsheet

```bash
# Maven
mvn clean install                      # Build with tests
mvn clean package -DskipTests          # Build without tests
mvn test                               # Run tests
mvn -pl patient-service test           # Test single module

# Docker
docker ps                              # List running containers
docker logs -f <container>             # Follow container logs
docker exec -it <container> bash       # Open shell in container
docker-compose up -d                   # Start all services
docker-compose down                    # Stop all services

# Kubernetes
kubectl apply -f <file>                # Deploy manifest
kubectl get pods -n health             # List pods
kubectl logs <pod> -n health           # View pod logs
kubectl delete pod <pod> -n health     # Delete pod
kubectl port-forward <svc> 8080:8080   # Port forward

# Database
mysql -h localhost -u root -p          # Connect to MySQL
SHOW DATABASES;                        # List databases
USE <db>; SELECT * FROM <table>;       # Query data

# Frontend
npm install                            # Install dependencies
npm run dev                            # Development server
npm run build                          # Production build
npm run preview                        # Preview production build
```

---

## Architecture Diagrams

### Service Communication

```
┌──────────────────┐
│     Frontend     │ (Port 5173)
│   React/Vite    │
└────────┬─────────┘
         │
         ├─────────────────────┬──────────────────┐
         │                     │                  │
    ┌────▼─────┐         ┌─────▼────┐      ┌─────▼────┐
    │  Auth    │         │ Patient  │      │Appointment│
    │ Service  │         │ Service  │      │ Service   │
    │ (8082)   │         │ (8082)   │      │ (8083)    │
    └────┬─────┘         └────┬─────┘      └─────┬─────┘
         │                    │                   │
         ├────────────────────┼───────────────────┤
         │                    │                   │
    ┌────▼────────────────────▼────────────────────▼─────┐
    │              MySQL 8.0.33                          │
    │  (userdb, patientdb, appointmentdb)               │
    └─────────────────────────────────────────────────────┘
    
    ┌──────────────────────────────────────────────────────┐
    │              Kafka (Event Bus)                       │
    │          (appointment events)                       │
    └──────────────────────────────────────────────────────┘
```

### Observability Stack

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Services   │    │    Logs      │    │   Metrics    │
│ (OpenTel)    │───▶│  (JSON)      │    │ (Prometheus) │
└──────────────┘    └──────────────┘    └──────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│         Jaeger (Traces)                 │
│      http://localhost:16686             │
└──────────────────────────────────────────┘
```

---

## Project Structure

```
HealthInsuranceApplication/
├── user-service/
│   ├── src/main/java/com/health/user/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── security/
│   ├── src/test/java/
│   └── pom.xml
├── patient-service/
│   ├── src/main/java/com/health/patient/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── entity/
│   │   └── repository/
│   ├── src/test/java/
│   └── pom.xml
├── appointment-service/
│   ├── src/main/java/com/health/appointment/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── event/
│   │   ├── entity/
│   │   └── repository/
│   ├── src/test/java/
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   └── api/
│   ├── index.html
│   ├── vite.config.ts
│   └── package.json
├── k8s/
│   ├── mysql.yaml
│   ├── kafka.yaml
│   ├── jaeger.yaml
│   ├── prometheus.yaml
│   ├── user-service.yaml
│   ├── patient-service.yaml
│   └── appointment-service.yaml
├── docker-compose.yml
├── RCA_DEMO.md
└── DEVELOPER_GUIDE.md (this file)
```

---

## Additional Resources

- [Spring Boot 3.1 Documentation](https://docs.spring.io/spring-boot/docs/3.1.4/reference/html/)
- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/instrumentation/java/)
- [Jaeger Distributed Tracing](https://www.jaegertracing.io/docs/)
- [Prometheus Monitoring](https://prometheus.io/docs/prometheus/latest/getting_started/)
- [React Documentation](https://react.dev)
- [Kubernetes Documentation](https://kubernetes.io/docs/)

---

## Contributing

1. Create feature branch: `git checkout -b feature/my-feature`
2. Make changes with tests
3. Run tests: `mvn test`
4. Commit with descriptive message: `git commit -m "feat: add new feature"`
5. Push to branch: `git push origin feature/my-feature`
6. Open pull request

---

## Support

For issues or questions:
1. Check RCA_DEMO.md for debugging guides
2. Review logs and traces in Jaeger
3. Check metrics in Prometheus
4. Open an issue with logs and trace ID

**Last Updated**: 2026-06-01
