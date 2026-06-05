package com.health.patient.service.impl;

import com.health.patient.dto.*;
import com.health.patient.entity.Patient;
import com.health.patient.repository.PatientRepository;
import com.health.patient.service.PatientService;
import com.health.patient.security.JwtUtil;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("patient-service");

    @Override
    @Transactional
    public PatientDto register(RegisterRequest request) {
        Span span = tracer.spanBuilder("patient.register").startSpan();
        try (var scope = span.makeCurrent()) {
            log.info("Attempting to register patient with email: {}", request.getEmail());
            span.setAttribute("patient.email", request.getEmail())
                    .setAttribute("patient.firstName", request.getFirstName());
            
            if (patientRepository.existsByEmail(request.getEmail())) {
                log.warn("Registration failed: Email already registered: {}", request.getEmail());
                span.recordException(new RuntimeException("Email already registered"));
                throw new RuntimeException("Email already registered");
            }
            
            Patient p = new Patient();
            p.setFirstName(request.getFirstName());
            p.setLastName(request.getLastName());
            p.setEmail(request.getEmail());
            p.setPassword(passwordEncoder.encode(request.getPassword()));
            p.setPhoneNumber(request.getPhoneNumber());
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            
            Patient saved = patientRepository.save(p);
            PatientDto dto = toDto(saved);
            
            log.info("Patient registered successfully with id: {}", saved.getId());
            span.addEvent("patient.registered", io.opentelemetry.api.common.Attributes.builder()
                    .put("patient.id", saved.getId())
                    .build());
            
            return dto;
        } catch (Exception ex) {
            span.recordException(ex);
            log.error("Patient registration failed", ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public String login(LoginRequest request) {
        Span span = tracer.spanBuilder("patient.login").startSpan();
        try (var scope = span.makeCurrent()) {
            log.info("Patient login attempt for email: {}", request.getEmail());
            span.setAttribute("patient.email", request.getEmail());
            
            Patient p = patientRepository.findByEmail(request.getEmail()).orElseThrow(() -> {
                log.warn("Login failed: Patient not found with email: {}", request.getEmail());
                return new RuntimeException("Invalid credentials");
            });
            
            if (!passwordEncoder.matches(request.getPassword(), p.getPassword())) {
                log.warn("Login failed: Invalid password for email: {}", request.getEmail());
                throw new RuntimeException("Invalid credentials");
            }
            
            String token = jwtUtil.generateToken(p.getEmail());
            log.info("Patient login successful for email: {}", request.getEmail());
            span.addEvent("patient.login.success", io.opentelemetry.api.common.Attributes.builder()
                    .put("patient.id", p.getId())
                    .build());
            
            return token;
        } catch (Exception ex) {
            span.recordException(ex);
            log.error("Patient login failed", ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public PatientDto getProfile(Long id) {
        Span span = tracer.spanBuilder("patient.getProfile").startSpan();
        try (var scope = span.makeCurrent()) {
            span.setAttribute("patient.id", id);
            log.debug("Fetching patient profile for id: {}", id);
            
            Patient p = patientRepository.findById(id).orElseThrow(() -> {
                log.warn("Patient profile not found for id: {}", id);
                return new RuntimeException("Patient not found");
            });
            
            PatientDto dto = toDto(p);
            log.debug("Patient profile fetched successfully for id: {}", id);
            return dto;
        } catch (Exception ex) {
            span.recordException(ex);
            log.error("Failed to fetch patient profile", ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    @Transactional
    public PatientDto updateProfile(Long id, PatientDto dto) {
        Patient p = patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        p.setFirstName(dto.getFirstName());
        p.setLastName(dto.getLastName());
        p.setPhoneNumber(dto.getPhoneNumber());
        p.setUpdatedAt(Instant.now());
        Patient saved = patientRepository.save(p);
        return toDto(saved);
    }

    @Override
    public void deleteProfile(Long id) {
        if (!patientRepository.existsById(id)) throw new RuntimeException("Patient not found");
        patientRepository.deleteById(id);
    }

    private PatientDto toDto(Patient p) {
        PatientDto dto = new PatientDto();
        dto.setId(p.getId());
        dto.setFirstName(p.getFirstName());
        dto.setLastName(p.getLastName());
        dto.setEmail(p.getEmail());
        dto.setPhoneNumber(p.getPhoneNumber());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}
