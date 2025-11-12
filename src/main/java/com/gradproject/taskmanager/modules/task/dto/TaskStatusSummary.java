package com.gradproject.taskmanager.modules.task.dto;


public record TaskStatusSummary(
    Long id,
    String name,
    String color,        
    String category      
) {}
