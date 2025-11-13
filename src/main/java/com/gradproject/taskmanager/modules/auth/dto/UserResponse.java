package com.gradproject.taskmanager.modules.auth.dto;

public record UserResponse(
    Integer id,
    String username,
    String email,
    String firstName,
    String lastName,
    String avatarUrl,
    String tsCreated,
    String tsUpdated
) {}
