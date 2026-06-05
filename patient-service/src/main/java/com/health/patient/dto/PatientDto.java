package com.health.patient.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class PatientDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Instant createdAt;
    private Instant updatedAt;
}
