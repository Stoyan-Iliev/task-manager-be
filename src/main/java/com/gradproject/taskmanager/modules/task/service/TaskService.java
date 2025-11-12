package com.gradproject.taskmanager.modules.task.service;

import com.gradproject.taskmanager.modules.task.dto.*;

import java.util.List;


public interface TaskService {

    
    TaskResponse createTask(Long projectId, TaskCreateRequest request, Integer userId);

    
    TaskResponse updateTask(Long taskId, TaskUpdateRequest request, Integer userId);

    
    void deleteTask(Long taskId, Integer userId);

    
    TaskResponse getTask(Long taskId, Integer userId);

    
    TaskResponse getTaskByKey(Long orgId, String key, Integer userId);

    
    List<TaskSummary> listProjectTasks(
        Long projectId,
        Long statusId,
        Integer assigneeId,
        Long sprintId,
        Integer userId
    );

    
    TaskResponse assignTask(Long taskId, TaskAssignRequest request, Integer userId);

    
    TaskResponse transitionStatus(Long taskId, TaskTransitionRequest request, Integer userId);

    
    List<TaskSummary> getMyOpenTasks(Long orgId, Integer userId);

    
    List<TaskSummary> getSubtasks(Long parentTaskId, Integer userId);

    
    List<TaskSummary> getBacklogTasks(Long projectId, Integer userId);
}
