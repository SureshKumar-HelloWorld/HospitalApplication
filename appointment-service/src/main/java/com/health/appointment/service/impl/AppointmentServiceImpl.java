package com.health.appointment.service.impl;

import com.health.appointment.dto.AppointmentDto;
import com.health.appointment.dto.AppointmentRequest;
import com.health.appointment.entity.Appointment;
import com.health.appointment.entity.AppointmentStatus;
import com.health.appointment.entity.Doctor;
import com.health.appointment.event.AppointmentEventProducer;
import com.health.appointment.repository.AppointmentRepository;
import com.health.appointment.repository.DoctorRepository;
import com.health.appointment.service.AppointmentService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentEventProducer eventProducer;
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("appointment-service");

    @Override
    @Transactional
    public AppointmentDto bookAppointment(AppointmentRequest request) {
        Span span = tracer.spanBuilder("appointment.book").startSpan();
        try (var scope = span.makeCurrent()) {
            log.info("Attempting to book appointment for doctor: {}, patient: {}", 
                    request.getDoctorId(), request.getPatientId());
            span.setAttribute("appointment.doctor.id", request.getDoctorId())
                    .setAttribute("appointment.patient.id", request.getPatientId())
                    .setAttribute("appointment.date", request.getAppointmentDate().toString())
                    .setAttribute("appointment.time", request.getAppointmentTime().toString());
            
            Doctor doctor = doctorRepository.findById(request.getDoctorId()).orElseThrow(() -> {
                log.warn("Doctor not found: {}", request.getDoctorId());
                return new RuntimeException("Doctor not found");
            });
            
            long count = appointmentRepository.countByDoctorAndDateTimeAndStatus(
                    doctor.getId(), request.getAppointmentDate(), request.getAppointmentTime(), AppointmentStatus.BOOKED);
            
            if (count > 0) {
                log.warn("Time slot not available for doctor: {}, date: {}, time: {}", 
                        doctor.getId(), request.getAppointmentDate(), request.getAppointmentTime());
                throw new RuntimeException("Doctor already booked for selected slot");
            }

            Appointment a = new Appointment();
            a.setPatientId(request.getPatientId());
            a.setDoctor(doctor);
            a.setAppointmentDate(request.getAppointmentDate());
            a.setAppointmentTime(request.getAppointmentTime());
            a.setStatus(AppointmentStatus.BOOKED);
            Appointment saved = appointmentRepository.save(a);

            eventProducer.publishAppointmentBooked(saved);
            
            log.info("Appointment booked successfully: {}", saved.getId());
            span.addEvent("appointment.booked", io.opentelemetry.api.common.Attributes.builder()
                    .put("appointment.id", saved.getId())
                    .build());

            return toDto(saved);
        } catch (Exception ex) {
            span.recordException(ex);
            log.error("Failed to book appointment", ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public List<AppointmentDto> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<AppointmentDto> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelAppointment(Long appointmentId) {
        Span span = tracer.spanBuilder("appointment.cancel").startSpan();
        try (var scope = span.makeCurrent()) {
            log.info("Attempting to cancel appointment: {}", appointmentId);
            span.setAttribute("appointment.id", appointmentId);
            
            Appointment a = appointmentRepository.findById(appointmentId).orElseThrow(() -> {
                log.warn("Appointment not found: {}", appointmentId);
                return new RuntimeException("Appointment not found");
            });
            
            a.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(a);
            eventProducer.publishAppointmentCancelled(a);
            
            log.info("Appointment cancelled successfully: {}", appointmentId);
            span.addEvent("appointment.cancelled");
        } catch (Exception ex) {
            span.recordException(ex);
            log.error("Failed to cancel appointment", ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    @Transactional
    public void completeAppointment(Long appointmentId) {
        Appointment a = appointmentRepository.findById(appointmentId).orElseThrow(() -> new RuntimeException("Appointment not found"));
        a.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(a);
    }

    @Override
    public Page<AppointmentDto> listAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable).map(this::toDto);
    }

    private AppointmentDto toDto(Appointment a) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(a.getId());
        dto.setPatientId(a.getPatientId());
        dto.setDoctorId(a.getDoctor()!=null? a.getDoctor().getId(): null);
        dto.setDoctorName(a.getDoctor()!=null? a.getDoctor().getName(): null);
        dto.setAppointmentDate(a.getAppointmentDate());
        dto.setAppointmentTime(a.getAppointmentTime());
        dto.setStatus(a.getStatus());
        return dto;
    }
}
