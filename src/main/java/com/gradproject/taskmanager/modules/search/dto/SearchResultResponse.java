package com.gradproject.taskmanager.modules.search.dto;

import java.util.List;


public record SearchResultResponse(
    String entityType,               
    Long entityId,
    String title,                    
    String description,              
    String status,                   
    Float relevanceScore,            
    List<String> highlightedFields,  
    String projectName,              
    Long projectId,                  
    String assigneeName,             
    Integer assigneeId               
) {}
