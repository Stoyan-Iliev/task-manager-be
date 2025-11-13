package com.gradproject.taskmanager.modules.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
    @Size(max = 50)
    String firstName,

    @Size(max = 50)
    String lastName,

    @Size(max = 100)
    String jobTitle,

    @Size(max = 100)
    String department,

    @Size(max = 20)
    @Pattern(
        regexp = "^[+]?[0-9]{0,20}$",
        message = "Phone must contain only digits and optional leading +"
    )
    String phone,

    @Size(max = 50)
    String timezone,

    @Size(max = 10)
    @Pattern(
        regexp = "^[a-z]{2}(-[A-Z]{2})?$",
        message = "Language must be a valid language code (e.g., en, es, en-US)"
    )
    String language,

    @Size(max = 20)
    String dateFormat,

    @Pattern(
        regexp = "^(12h|24h)$",
        message = "Time format must be either 12h or 24h"
    )
    String timeFormat,

    @Size(max = 500)
    String bio
) {}
