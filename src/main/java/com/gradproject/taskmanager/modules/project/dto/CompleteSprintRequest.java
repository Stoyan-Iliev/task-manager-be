package com.gradproject.taskmanager.modules.project.dto;


public record CompleteSprintRequest(
    Boolean rolloverIncompleteTasks,
    Long targetSprintId
) {
    
    public CompleteSprintRequest {
        
        if (rolloverIncompleteTasks == null) {
            rolloverIncompleteTasks = true;
        }
    }

    
    public static CompleteSprintRequest rolloverToBacklog() {
        return new CompleteSprintRequest(true, null);
    }

    
    public static CompleteSprintRequest rolloverToSprint(Long targetSprintId) {
        return new CompleteSprintRequest(true, targetSprintId);
    }

    
    public static CompleteSprintRequest keepInSprint() {
        return new CompleteSprintRequest(false, null);
    }
}
