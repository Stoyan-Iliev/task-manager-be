package com.gradproject.taskmanager.modules.project.service;

import com.gradproject.taskmanager.modules.project.dto.ReorderStatusesRequest;
import com.gradproject.taskmanager.modules.project.dto.StatusTemplateResponse;
import com.gradproject.taskmanager.modules.project.dto.TaskStatusRequest;
import com.gradproject.taskmanager.modules.project.dto.TaskStatusResponse;

import java.util.List;


public interface TaskStatusService {

    
    TaskStatusResponse createStatus(Long projectId, TaskStatusRequest request, Integer userId);

    
    TaskStatusResponse updateStatus(Long statusId, TaskStatusRequest request, Integer userId);

    
    void deleteStatus(Long statusId, Integer userId);

    
    List<TaskStatusResponse> reorderStatuses(Long projectId, ReorderStatusesRequest request, Integer userId);

    
    List<TaskStatusResponse> listProjectStatuses(Long projectId, Integer requesterId);

    
    List<StatusTemplateResponse> getStatusTemplates();
}
