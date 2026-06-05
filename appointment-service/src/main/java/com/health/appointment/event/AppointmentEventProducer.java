package com.health.appointment.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.appointment.entity.Appointment;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AppointmentEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(AppointmentEventProducer.class);

    public void publishAppointmentBooked(Appointment appointment) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("appointmentId", appointment.getId());
            payload.put("patientId", appointment.getPatientId());
            payload.put("doctorId", appointment.getDoctor()!=null? appointment.getDoctor().getId(): null);
            payload.put("date", appointment.getAppointmentDate().toString());
            payload.put("time", appointment.getAppointmentTime().toString());
            payload.put("status", appointment.getStatus().name());
            String msg = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("appointments", msg);
            log.info("Published AppointmentBooked event for id={}", appointment.getId());
        } catch (Exception ex) {
            log.error("Failed to publish appointment event", ex);
        }
    }

    public void publishAppointmentCancelled(Appointment appointment) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("appointmentId", appointment.getId());
            payload.put("patientId", appointment.getPatientId());
            payload.put("status", appointment.getStatus().name());
            String msg = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("appointments", msg);
            log.info("Published AppointmentCancelled event for id={}", appointment.getId());
        } catch (Exception ex) {
            log.error("Failed to publish appointment cancelled event", ex);
        }
    }
}
