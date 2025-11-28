package com.gradproject.taskmanager.modules.project.dto;

import com.gradproject.taskmanager.modules.project.domain.SprintStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


public record SprintResponse(
    Long id,
    Long projectId,
    String name,
    String goal,
    LocalDate startDate,
    LocalDate endDate,
    SprintStatus status,
    LocalDateTime createdAt,
    String createdByUsername,
    LocalDateTime completedAt,
    String completedByUsername,
    BigDecimal capacityHours,
    SprintMetrics metrics
) {}
