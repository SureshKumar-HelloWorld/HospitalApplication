package com.health.user.dto;

import lombok.Data;
import java.time.Instant;
import java.util.Set;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Instant createdAt;
    private Set<String> roles;
}
