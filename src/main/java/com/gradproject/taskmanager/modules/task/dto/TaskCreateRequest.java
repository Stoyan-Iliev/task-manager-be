package com.gradproject.taskmanager.modules.task.dto;

import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import com.gradproject.taskmanager.modules.task.domain.TaskType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;


public record TaskCreateRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    String title,

    String description,  

    @NotNull(message = "Status is required")
    Long statusId,

    Integer assigneeId,  

    Long sprintId,  

    Long parentTaskId,  

    @NotNull(message = "Task type is required")
    TaskType type,

    @NotNull(message = "Priority is required")
    TaskPriority priority,

    LocalDate dueDate,  

    @DecimalMin(value = "0.0", message = "Estimated hours must be positive")
    BigDecimal estimatedHours,  

    @Min(value = 1, message = "Story points must be at least 1")
    Integer storyPoints  
) {
    
    public TaskCreateRequest {
        if (type == null) {
            type = TaskType.TASK;
        }
        if (priority == null) {
            priority = TaskPriority.MEDIUM;
        }
    }
}
