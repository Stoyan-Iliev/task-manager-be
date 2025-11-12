package com.gradproject.taskmanager.modules.task.dto;

import jakarta.validation.constraints.NotNull;


public record TaskTransitionRequest(
    @NotNull(message = "New status ID is required")
    Long newStatusId,

    String comment  
) {}
