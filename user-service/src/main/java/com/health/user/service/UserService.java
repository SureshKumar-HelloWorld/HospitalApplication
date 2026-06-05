package com.health.user.service;

import com.health.user.dto.AuthResponse;
import com.health.user.dto.LoginRequest;
import com.health.user.dto.RegisterRequest;
import com.health.user.dto.UserDto;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserDto getProfile(String username);
    UserDto updateProfile(String username, UserDto dto);
    UserDto findById(Long id);
}
