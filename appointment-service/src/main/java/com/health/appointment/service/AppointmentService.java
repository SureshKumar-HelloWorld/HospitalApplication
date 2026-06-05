package com.health.appointment.service;

import com.health.appointment.dto.AppointmentDto;
import com.health.appointment.dto.AppointmentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AppointmentService {
    AppointmentDto bookAppointment(AppointmentRequest request);
    List<AppointmentDto> getAppointmentsByPatient(Long patientId);
    List<AppointmentDto> getAppointmentsByDoctor(Long doctorId);
    void cancelAppointment(Long appointmentId);
    void completeAppointment(Long appointmentId);
    Page<AppointmentDto> listAll(Pageable pageable);
}
