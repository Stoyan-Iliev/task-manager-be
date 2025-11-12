package com.gradproject.taskmanager.modules.search.dto;

import java.time.LocalDateTime;
import java.util.Map;


public record SavedSearchResponse(
    Long id,
    String name,
    String description,
    String entityType,
    Map<String, Object> queryParams,
    Boolean isShared,
    Integer userId,
    String userName,
    Long organizationId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
