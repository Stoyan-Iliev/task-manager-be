package com.gradproject.taskmanager.modules.activity.dto;

import com.gradproject.taskmanager.modules.activity.domain.ActionType;
import com.gradproject.taskmanager.modules.activity.domain.EntityType;
import com.gradproject.taskmanager.shared.dto.UserSummary;

import java.time.Instant;


public record ActivityLogResponse(
        Long id,
        EntityType entityType,
        Long entityId,
        ActionType action,
        UserSummary user,
        String fieldName,
        String oldValue,
        String newValue,
        String metadata,
        Integer versionNumber,
        Instant timestamp
) {
}
