package com.gradproject.taskmanager.shared.dto;

import com.gradproject.taskmanager.modules.task.domain.TaskPriority;

import java.time.LocalDate;
import java.time.LocalDateTime;


public record TaskSummary(
    Long id,
    String key,              
    String title,
    String statusName,       
    TaskPriority priority,
    UserSummary assignee,    
    LocalDate dueDate,       
    LocalDateTime updatedAt
) {}
