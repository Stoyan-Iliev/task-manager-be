package com.gradproject.taskmanager.modules.task.dto;

import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import com.gradproject.taskmanager.shared.dto.UserSummary;


public record TaskSummary(
    Long id,
    String key,              
    String title,
    TaskStatusSummary status,
    UserSummary assignee,    
    TaskPriority priority,
    boolean isOverdue
) {}
