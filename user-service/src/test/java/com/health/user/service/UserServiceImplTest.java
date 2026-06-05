package com.health.user.service;

import com.health.user.dto.LoginRequest;
import com.health.user.dto.RegisterRequest;
import com.health.user.dto.UserDto;
import com.health.user.entity.Role;
import com.health.user.entity.User;
import com.health.user.repository.RoleRepository;
import com.health.user.repository.UserRepository;
import com.health.user.security.JwtUtil;
import com.health.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("john_doe");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("john_doe");
        loginRequest.setPassword("password123");

        testRole = new Role();
        testRole.setId(1);
        testRole.setName("ROLE_USER");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("john@example.com");
        testUser.setRoles(new HashSet<>(Set.of(testRole)));
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(testRole));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.register(registerRequest);

        assertNotNull(result);
        assertEquals("john_doe", result.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateUsername() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.register(registerRequest));
        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(testUser.getUsername())).thenReturn("jwt-token-123");

        String token = userService.login(loginRequest);

        assertNotNull(token);
        assertEquals("jwt-token-123", token);
        verify(jwtUtil, times(1)).generateToken(testUser.getUsername());
    }

    @Test
    void testLoginInvalidPassword() {
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.login(loginRequest));
        assertEquals("Invalid credentials", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void testLoginUserNotFound() {
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.login(loginRequest));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void testGetUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("john_doe", result.getUsername());
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetUserByIdNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.getUserById(999L));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testUpdateUser() {
        UserDto updateDto = new UserDto();
        updateDto.setUsername("jane_doe");
        updateDto.setEmail("jane@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.updateUser(1L, updateDto);

        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testDeleteUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteUserNotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.deleteUser(999L));
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).deleteById(any());
    }
}
