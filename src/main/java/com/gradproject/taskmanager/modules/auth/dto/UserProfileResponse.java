package com.gradproject.taskmanager.modules.auth.dto;

public record UserProfileResponse(
    Integer id,
    String username,
    String email,
    String firstName,
    String lastName,
    String avatarUrl,
    String jobTitle,
    String department,
    String phone,
    String timezone,
    String language,
    String dateFormat,
    String timeFormat,
    String bio,
    boolean enabled,
    boolean locked,
    String tsCreated,
    String tsUpdated
) {}
