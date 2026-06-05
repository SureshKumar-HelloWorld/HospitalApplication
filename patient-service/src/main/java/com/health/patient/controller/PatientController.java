package com.health.patient.controller;

import com.health.patient.dto.PatientDto;
import com.health.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    @GetMapping("/{id}")
    public ResponseEntity<PatientDto> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getProfile(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientDto> updateProfile(@PathVariable Long id, @RequestBody PatientDto dto) {
        return ResponseEntity.ok(patientService.updateProfile(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        patientService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }
}
