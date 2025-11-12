package com.gradproject.taskmanager.modules.organization.dto;

import com.gradproject.taskmanager.modules.organization.domain.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record MemberInviteRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotNull(message = "Role is required")
    OrganizationRole role
) {}
