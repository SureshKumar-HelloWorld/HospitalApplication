package com.health.user.service.impl;

import com.health.user.dto.*;
import com.health.user.entity.Role;
import com.health.user.entity.User;
import com.health.user.mapper.UserMapperImpl;
import com.health.user.repository.RoleRepository;
import com.health.user.repository.UserRepository;
import com.health.user.service.UserService;
import com.health.user.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapperImpl mapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        Set<Role> roles = new HashSet<>();
        Role patientRole = roleRepository.findByName("PATIENT").orElseGet(() -> roleRepository.save(new Role(null, "PATIENT")));
        roles.add(patientRole);
        user.setRoles(roles);

        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getUsername());
        return new AuthResponse(token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );
        String username = auth.getName();
        String token = jwtUtil.generateToken(username);
        return new AuthResponse(token);
    }

    @Override
    public UserDto getProfile(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return mapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto updateProfile(String username, UserDto dto) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        userRepository.save(user);
        return mapper.toDto(user);
    }

    @Override
    public UserDto findById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return mapper.toDto(user);
    }
}
