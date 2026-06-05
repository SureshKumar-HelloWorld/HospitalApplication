package com.health.patient.controller;

import com.health.patient.dto.*;
import com.health.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final PatientService patientService;

    @PostMapping("/register")
    public ResponseEntity<PatientDto> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(patientService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        String token = patientService.login(request);
        return ResponseEntity.ok(token);
    }
}
