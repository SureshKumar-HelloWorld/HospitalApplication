package com.health.appointment.service;

import com.health.appointment.dto.AppointmentDto;
import com.health.appointment.dto.AppointmentRequest;
import com.health.appointment.entity.Appointment;
import com.health.appointment.entity.AppointmentStatus;
import com.health.appointment.entity.Doctor;
import com.health.appointment.event.AppointmentEventProducer;
import com.health.appointment.repository.AppointmentRepository;
import com.health.appointment.repository.DoctorRepository;
import com.health.appointment.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private AppointmentEventProducer eventProducer;
    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private AppointmentRequest appointmentRequest;
    private Doctor testDoctor;
    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
        appointmentRequest = new AppointmentRequest();
        appointmentRequest.setDoctorId(1L);
        appointmentRequest.setAppointmentDate(LocalDate.of(2026, 6, 15));
        appointmentRequest.setAppointmentTime(LocalTime.of(10, 0));
        appointmentRequest.setPatientId(1L);

        testDoctor = new Doctor();
        testDoctor.setId(1L);
        testDoctor.setName("Dr. Smith");
        testDoctor.setSpecialty("Cardiology");
        testDoctor.setCreatedAt(Instant.now());

        testAppointment = new Appointment();
        testAppointment.setId(1L);
        testAppointment.setDoctor(testDoctor);
        testAppointment.setPatientId(1L);
        testAppointment.setAppointmentDate(LocalDate.of(2026, 6, 15));
        testAppointment.setAppointmentTime(LocalTime.of(10, 0));
        testAppointment.setStatus(AppointmentStatus.BOOKED);
    }

    @Test
    void testBookAppointmentSuccess() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(appointmentRepository.countByDoctorAndDateTimeAndStatus(
                1L, LocalDate.of(2026, 6, 15), LocalTime.of(10, 0), AppointmentStatus.BOOKED))
                .thenReturn(0L);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        AppointmentDto result = appointmentService.bookAppointment(appointmentRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(eventProducer, times(1)).publishAppointmentBooked(any());
    }

    @Test
    void testBookAppointmentDoctorNotFound() {
        when(doctorRepository.findById(999L)).thenReturn(Optional.empty());

        appointmentRequest.setDoctorId(999L);
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> appointmentService.bookAppointment(appointmentRequest));
        assertTrue(exception.getMessage().contains("Doctor not found"));
    }

    @Test
    void testBookAppointmentSlotNotAvailable() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(appointmentRepository.countByDoctorAndDateTimeAndStatus(
                1L, LocalDate.of(2026, 6, 15), LocalTime.of(10, 0), AppointmentStatus.BOOKED))
                .thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> appointmentService.bookAppointment(appointmentRequest));
        assertTrue(exception.getMessage().contains("already booked"));
    }

    @Test
    void testGetAppointmentsByDoctor() {
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(appointmentRepository.findByDoctorId(1L)).thenReturn(appointments);

        List<AppointmentDto> result = appointmentService.getAppointmentsByDoctor(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appointmentRepository, times(1)).findByDoctorId(1L);
    }

    @Test
    void testGetAppointmentsByPatient() {
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(appointmentRepository.findByPatientId(1L)).thenReturn(appointments);

        List<AppointmentDto> result = appointmentService.getAppointmentsByPatient(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appointmentRepository, times(1)).findByPatientId(1L);
    }

    @Test
    void testCancelAppointmentSuccess() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        appointmentService.cancelAppointment(1L);

        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(eventProducer, times(1)).publishAppointmentCancelled(any());
    }

    @Test
    void testCancelAppointmentNotFound() {
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> appointmentService.cancelAppointment(999L));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testCompleteAppointment() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        appointmentService.completeAppointment(1L);

        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    void testListAllAppointments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Appointment> appointmentPage = new PageImpl<>(Arrays.asList(testAppointment), pageable, 1);
        when(appointmentRepository.findAll(pageable)).thenReturn(appointmentPage);

        Page<AppointmentDto> result = appointmentService.listAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(appointmentRepository, times(1)).findAll(pageable);
    }
}
