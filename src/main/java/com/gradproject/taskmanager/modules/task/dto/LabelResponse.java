package com.gradproject.taskmanager.modules.task.dto;

import java.time.LocalDateTime;


public record LabelResponse(
        Long id,
        Long organizationId,
        String name,
        String color,
        String description,
        LocalDateTime createdAt,
        Integer createdBy
) {
}
