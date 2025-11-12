package com.gradproject.taskmanager.modules.project.dto;

import java.time.LocalDateTime;


public record ProjectResponse(
    Long id,
    Long organizationId,
    String key,
    String name,
    String description,
    TaskStatusSummary defaultStatus,
    LocalDateTime createdAt,
    String createdByUsername,
    Long memberCount,
    Long statusCount
) {}
