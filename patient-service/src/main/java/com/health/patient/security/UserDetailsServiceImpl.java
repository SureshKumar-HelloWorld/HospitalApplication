package com.health.patient.security;

import com.health.patient.entity.Patient;
import com.health.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final PatientRepository patientRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Patient p = patientRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("Patient not found"));
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_PATIENT"));
        return new org.springframework.security.core.userdetails.User(p.getEmail(), p.getPassword(), authorities);
    }
}
