package com.health.appointment.controller;

import com.health.appointment.dto.DoctorDto;
import com.health.appointment.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorRepository doctorRepository;

    @GetMapping
    public ResponseEntity<List<DoctorDto>> list() {
        List<DoctorDto> dtos = doctorRepository.findAll().stream().map(d -> {
            DoctorDto dto = new DoctorDto();
            dto.setId(d.getId()); dto.setName(d.getName()); dto.setSpecialization(d.getSpecialization());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorDto> get(@PathVariable Long id) {
        var d = doctorRepository.findById(id).orElseThrow(() -> new RuntimeException("Doctor not found"));
        DoctorDto dto = new DoctorDto(); dto.setId(d.getId()); dto.setName(d.getName()); dto.setSpecialization(d.getSpecialization());
        return ResponseEntity.ok(dto);
    }
}
