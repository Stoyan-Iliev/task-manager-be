package com.gradproject.taskmanager.shared.dto;


public record UserSummary(
    Integer id,
    String username,
    String email,
    String firstName,
    String lastName,
    String avatarUrl
) {}
