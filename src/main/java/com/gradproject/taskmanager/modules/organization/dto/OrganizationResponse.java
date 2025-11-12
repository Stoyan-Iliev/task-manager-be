package com.gradproject.taskmanager.modules.organization.dto;

import java.time.LocalDateTime;


public record OrganizationResponse(
    Long id,
    String name,
    String slug,
    String description,
    Long memberCount,
    LocalDateTime createdAt,
    String createdByUsername
) {}
