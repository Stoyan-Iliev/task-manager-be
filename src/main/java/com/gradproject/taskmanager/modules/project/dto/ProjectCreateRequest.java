package com.gradproject.taskmanager.modules.project.dto;

import com.gradproject.taskmanager.modules.project.domain.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record ProjectCreateRequest(
    @NotBlank(message = "Project key is required")
    @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "Project key must be 2-10 uppercase alphanumeric characters")
    String key,

    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 255, message = "Project name must be between 3 and 255 characters")
    String name,

    @NotNull(message = "Project type is required")
    ProjectType type,

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    String description,

    Long statusTemplateId
) {}
