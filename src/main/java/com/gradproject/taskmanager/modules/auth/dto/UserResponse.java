package com.gradproject.taskmanager.modules.auth.dto;

public record UserResponse(Integer id, String username, String email, String tsCreated, String tsUpdated) {}
