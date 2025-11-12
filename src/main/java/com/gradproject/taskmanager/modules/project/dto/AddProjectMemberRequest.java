package com.gradproject.taskmanager.modules.project.dto;

import com.gradproject.taskmanager.modules.project.domain.ProjectRole;
import jakarta.validation.constraints.NotNull;


public record AddProjectMemberRequest(
    @NotNull(message = "User ID is required")
    Integer userId,

    @NotNull(message = "Role is required")
    ProjectRole role
) {}
