package com.gradproject.taskmanager.modules.search.dto;

import java.util.Map;


public record SavedSearchRequest(
    String name,
    String description,
    String entityType,           
    Map<String, Object> queryParams,
    Boolean isShared
) {
    public SavedSearchRequest {
        if (isShared == null) {
            isShared = false;
        }
    }
}
