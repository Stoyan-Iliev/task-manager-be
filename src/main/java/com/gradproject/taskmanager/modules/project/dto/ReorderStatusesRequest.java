package com.gradproject.taskmanager.modules.project.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;


public record ReorderStatusesRequest(
    @NotEmpty(message = "Status IDs list cannot be empty")
    List<Long> statusIds
) {}
