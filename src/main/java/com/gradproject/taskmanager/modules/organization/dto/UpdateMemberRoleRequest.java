package com.gradproject.taskmanager.modules.organization.dto;

import com.gradproject.taskmanager.modules.organization.domain.OrganizationRole;
import jakarta.validation.constraints.NotNull;


public record UpdateMemberRoleRequest(
    @NotNull(message = "Role is required")
    OrganizationRole role
) {}
