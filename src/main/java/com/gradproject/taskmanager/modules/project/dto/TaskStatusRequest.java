package com.gradproject.taskmanager.modules.project.dto;

import com.gradproject.taskmanager.modules.project.domain.StatusCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record TaskStatusRequest(
    @NotBlank(message = "Status name is required")
    @Size(min = 2, max = 100, message = "Status name must be between 2 and 100 characters")
    String name,

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color must be a valid hex color code (e.g., #3b82f6)")
    String color,

    @NotNull(message = "Category is required")
    StatusCategory category
) {}
