package com.gradproject.taskmanager.modules.project.dto;

import com.gradproject.taskmanager.modules.project.domain.StatusCategory;

import java.time.LocalDateTime;

public record TaskStatusResponse(
    Long id,
    Long projectId,
    String name,
    String color,
    Integer orderIndex,
    StatusCategory category,
    Boolean isDefault,
    LocalDateTime createdAt
) {}
