package com.gradproject.taskmanager.modules.project.service;

import com.gradproject.taskmanager.modules.project.dto.CompleteSprintRequest;
import com.gradproject.taskmanager.modules.project.dto.SprintRequest;
import com.gradproject.taskmanager.modules.project.dto.SprintResponse;

import java.util.List;


public interface SprintService {

    
    SprintResponse createSprint(Long projectId, SprintRequest request, Integer userId);

    
    SprintResponse updateSprint(Long sprintId, SprintRequest request, Integer userId);

    
    void deleteSprint(Long sprintId, Integer userId);

    
    SprintResponse getSprint(Long sprintId, Integer requesterId);

    
    List<SprintResponse> listProjectSprints(Long projectId, Integer requesterId);

    
    SprintResponse startSprint(Long sprintId, Integer userId);

    
    SprintResponse completeSprint(Long sprintId, CompleteSprintRequest request, Integer userId);

    
    void assignTasksToSprint(Long sprintId, List<Long> taskIds, Integer userId);

    
    void removeTasksFromSprint(List<Long> taskIds, Integer userId);

    
    List<?> getSprintTasks(Long sprintId, Integer requesterId);
}
