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

    /**
     * Apply a status template to a project, replacing existing statuses.
     * @param projectId the project ID
     * @param templateId the template identifier (e.g., BASIC, SCRUM, KANBAN, SOFTWARE_DEVELOPMENT)
     * @param userId the requesting user's ID
     * @return the list of newly created statuses
     */
    List<TaskStatusResponse> applyStatusTemplate(Long projectId, String templateId, Integer userId);
}
