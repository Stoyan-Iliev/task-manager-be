package com.gradproject.taskmanager.modules.search.dto;

import java.time.LocalDate;
import java.util.List;


public record SearchRequest(
    String query,                    
    String entityType,               
    List<String> projectIds,         
    List<Integer> assigneeIds,       
    List<String> statuses,           
    List<Long> labelIds,             
    LocalDate dueDateFrom,           
    LocalDate dueDateTo,
    Integer priorityMin,             
    Integer priorityMax,
    Boolean includeArchived,         
    String sortBy,                   
    String sortDirection             
) {
    public SearchRequest {
        if (entityType == null || entityType.isBlank()) {
            entityType = "GLOBAL";
        }
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "relevance";
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "DESC";
        }
        if (includeArchived == null) {
            includeArchived = false;
        }
    }
}
