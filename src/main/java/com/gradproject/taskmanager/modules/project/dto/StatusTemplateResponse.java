package com.gradproject.taskmanager.modules.project.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


public record StatusTemplateResponse(
    Long id,
    String name,
    String description,
    List<Map<String, Object>> statuses,
    LocalDateTime createdAt
) {}
