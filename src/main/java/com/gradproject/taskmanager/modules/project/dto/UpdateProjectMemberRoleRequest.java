package com.gradproject.taskmanager.modules.project.dto;

import com.gradproject.taskmanager.modules.project.domain.ProjectRole;
import jakarta.validation.constraints.NotNull;


public record UpdateProjectMemberRoleRequest(
    @NotNull(message = "Role is required")
    ProjectRole role
) {}
