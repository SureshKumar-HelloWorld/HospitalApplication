package com.health.patient.service;

import com.health.patient.dto.LoginRequest;
import com.health.patient.dto.PatientDto;
import com.health.patient.dto.RegisterRequest;
import com.health.patient.entity.Patient;
import com.health.patient.repository.PatientRepository;
import com.health.patient.security.JwtUtil;
import com.health.patient.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private PatientServiceImpl patientService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Patient testPatient;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setPhoneNumber("555-1234");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        testPatient = new Patient();
        testPatient.setId(1L);
        testPatient.setFirstName("John");
        testPatient.setLastName("Doe");
        testPatient.setEmail("john@example.com");
        testPatient.setPassword("encodedPassword");
        testPatient.setPhoneNumber("555-1234");
        testPatient.setCreatedAt(Instant.now());
        testPatient.setUpdatedAt(Instant.now());
    }

    @Test
    void testRegisterSuccess() {
        when(patientRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);

        PatientDto result = patientService.register(registerRequest);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("john@example.com", result.getEmail());
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    void testRegisterDuplicateEmail() {
        when(patientRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> patientService.register(registerRequest));
        assertEquals("Email already registered", exception.getMessage());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void testLoginSuccess() {
        when(patientRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testPatient));
        when(passwordEncoder.matches(loginRequest.getPassword(), testPatient.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(testPatient.getEmail())).thenReturn("jwt-token-123");

        String token = patientService.login(loginRequest);

        assertNotNull(token);
        assertEquals("jwt-token-123", token);
        verify(jwtUtil, times(1)).generateToken(testPatient.getEmail());
    }

    @Test
    void testLoginInvalidPassword() {
        when(patientRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testPatient));
        when(passwordEncoder.matches(loginRequest.getPassword(), testPatient.getPassword())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> patientService.login(loginRequest));
        assertEquals("Invalid credentials", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void testLoginUserNotFound() {
        when(patientRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> patientService.login(loginRequest));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void testGetProfile() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatient));

        PatientDto result = patientService.getProfile(1L);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetProfileNotFound() {
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> patientService.getProfile(999L));
        assertEquals("Patient not found", exception.getMessage());
    }

    @Test
    void testUpdateProfile() {
        PatientDto updateDto = new PatientDto();
        updateDto.setFirstName("Jane");
        updateDto.setLastName("Smith");
        updateDto.setPhoneNumber("555-5678");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatient));
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);

        PatientDto result = patientService.updateProfile(1L, updateDto);

        assertNotNull(result);
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    void testDeleteProfile() {
        when(patientRepository.existsById(1L)).thenReturn(true);

        patientService.deleteProfile(1L);

        verify(patientRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteProfileNotFound() {
        when(patientRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> patientService.deleteProfile(999L));
        assertEquals("Patient not found", exception.getMessage());
        verify(patientRepository, never()).deleteById(any());
    }
}
