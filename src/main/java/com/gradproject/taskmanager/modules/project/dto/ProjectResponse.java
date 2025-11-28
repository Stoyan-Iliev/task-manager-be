package com.gradproject.taskmanager.modules.project.dto;

import com.gradproject.taskmanager.modules.project.domain.ProjectType;

import java.time.LocalDateTime;


public record ProjectResponse(
    Long id,
    Long organizationId,
    String key,
    String name,
    ProjectType type,
    String description,
    TaskStatusSummary defaultStatus,
    LocalDateTime createdAt,
    String createdByUsername,
    Long memberCount,
    Long statusCount,
    Long taskCount
) {}
