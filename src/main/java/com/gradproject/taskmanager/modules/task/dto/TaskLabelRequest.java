package com.gradproject.taskmanager.modules.task.dto;

import jakarta.validation.constraints.NotNull;


public record TaskLabelRequest(
        @NotNull(message = "Label ID is required")
        Long labelId
) {
}
