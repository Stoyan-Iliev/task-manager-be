package com.gradproject.taskmanager.modules.release.dto;

import jakarta.validation.constraints.NotNull;


public record AddTaskToReleaseRequest(
    @NotNull(message = "Task ID is required")
    Long taskId
) {}
