package com.gradproject.taskmanager.modules.release.dto;

import com.gradproject.taskmanager.modules.release.domain.enums.ReleaseStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;


public record ReleaseResponse(
    Long id,
    Long projectId,
    String projectName,
    String name,
    String description,
    String version,
    LocalDate releaseDate,
    ReleaseStatus status,
    Integer createdById,
    String createdByUsername,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime releasedAt,
    LocalDateTime archivedAt,
    Long taskCount,
    Long completedTaskCount
) {}
