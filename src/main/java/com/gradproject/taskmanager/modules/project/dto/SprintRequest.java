package com.gradproject.taskmanager.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;


public record SprintRequest(
    @NotBlank(message = "Sprint name is required")
    @Size(min = 2, max = 255, message = "Sprint name must be between 2 and 255 characters")
    String name,

    @Size(max = 2000, message = "Goal cannot exceed 2000 characters")
    String goal,

    LocalDate startDate,

    LocalDate endDate
) {
    
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}
