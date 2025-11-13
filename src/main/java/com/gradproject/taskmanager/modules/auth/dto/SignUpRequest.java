package com.gradproject.taskmanager.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @NotBlank
    @Size(min = 3, max = 50)
    String username,

    @NotBlank
    @Email
    @Size(max = 100)
    String email,

    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,100}$",
        message = "Password must be 8-100 characters and include upper, lower, and digit"
    )
    String password,

    @Size(max = 50)
    String firstName,

    @Size(max = 50)
    String lastName
) {}
