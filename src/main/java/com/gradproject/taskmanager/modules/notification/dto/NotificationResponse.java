package com.gradproject.taskmanager.modules.notification.dto;

import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.shared.dto.UserSummary;

import java.time.LocalDateTime;


public record NotificationResponse(
    Long id,
    NotificationType type,
    String title,
    String message,
    UserSummary actor,         
    Long taskId,                
    String taskKey,             
    String relatedEntityType,   
    Long relatedEntityId,       
    Boolean isRead,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}
