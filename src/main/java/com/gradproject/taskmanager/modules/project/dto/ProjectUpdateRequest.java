package com.gradproject.taskmanager.modules.project.dto;

import com.gradproject.taskmanager.modules.project.domain.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record ProjectUpdateRequest(
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 255, message = "Project name must be between 3 and 255 characters")
    String name,

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    String description,

    ProjectType type
) {}
