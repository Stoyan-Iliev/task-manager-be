package com.gradproject.taskmanager.modules.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record LabelRequest(
        @NotBlank(message = "Label name is required")
        @Size(min = 1, max = 50, message = "Label name must be between 1 and 50 characters")
        String name,

        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "Color must be a valid hex code (e.g., #3b82f6)"
        )
        String color,  

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description  
) {
    
    public String getColorOrDefault() {
        return color != null ? color : "#3b82f6";
    }

    
    public String normalizedName() {
        return name != null ? name.trim().toLowerCase() : null;
    }
}
