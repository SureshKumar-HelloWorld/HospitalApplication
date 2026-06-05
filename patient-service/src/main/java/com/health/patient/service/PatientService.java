package com.health.patient.service;

import com.health.patient.dto.*;

public interface PatientService {
    PatientDto register(RegisterRequest request);
    String login(LoginRequest request);
    PatientDto getProfile(Long id);
    PatientDto updateProfile(Long id, PatientDto dto);
    void deleteProfile(Long id);
}
