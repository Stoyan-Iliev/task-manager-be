package com.gradproject.taskmanager.modules.task.dto;

import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import com.gradproject.taskmanager.modules.task.domain.TaskType;
import com.gradproject.taskmanager.shared.dto.UserSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


public record TaskResponse(
    Long id,
    String key,               
    String title,
    String description,       
    TaskStatusSummary status,
    UserSummary assignee,     
    UserSummary reporter,
    TaskType type,
    TaskPriority priority,
    LocalDate dueDate,
    BigDecimal estimatedHours,
    BigDecimal loggedHours,
    Integer storyPoints,
    Long projectId,
    String projectKey,
    String projectName,
    Long sprintId,
    String sprintName,
    Long parentTaskId,
    String parentTaskKey,
    Integer subtaskCount,
    Integer commentCount,
    Integer attachmentCount,
    boolean isOverdue,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    UserSummary createdBy
) {}
