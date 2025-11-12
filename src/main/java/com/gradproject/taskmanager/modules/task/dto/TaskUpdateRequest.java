package com.gradproject.taskmanager.modules.task.dto;

import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;


public record TaskUpdateRequest(
    @Size(max = 500, message = "Title must not exceed 500 characters")
    String title,

    String description,

    TaskPriority priority,

    LocalDate dueDate,

    @DecimalMin(value = "0.0", message = "Estimated hours must be positive")
    BigDecimal estimatedHours,

    @Min(value = 1, message = "Story points must be at least 1")
    Integer storyPoints
) {}
