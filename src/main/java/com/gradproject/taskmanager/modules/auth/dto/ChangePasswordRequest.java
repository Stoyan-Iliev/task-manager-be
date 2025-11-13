package com.gradproject.taskmanager.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,100}$",
        message = "Password must be 8-100 characters and include upper, lower, and digit"
    )
    String newPassword,

    @NotBlank(message = "Password confirmation is required")
    String confirmPassword
) {}
