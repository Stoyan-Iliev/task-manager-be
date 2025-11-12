package com.gradproject.taskmanager.modules.project.dto;

import com.gradproject.taskmanager.modules.project.domain.StatusCategory;


public record TaskStatusSummary(
    Long id,
    String name,
    String color,
    StatusCategory category
) {}
