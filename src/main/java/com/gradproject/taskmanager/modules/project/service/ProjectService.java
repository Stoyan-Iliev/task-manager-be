package com.gradproject.taskmanager.modules.project.service;

import com.gradproject.taskmanager.modules.project.dto.ProjectCreateRequest;
import com.gradproject.taskmanager.modules.project.dto.ProjectResponse;
import com.gradproject.taskmanager.modules.project.dto.ProjectUpdateRequest;

import java.util.List;


public interface ProjectService {

    
    ProjectResponse createProject(Long organizationId, ProjectCreateRequest request, Integer userId);

    
    ProjectResponse updateProject(Long projectId, ProjectUpdateRequest request, Integer userId);

    
    void deleteProject(Long projectId, Integer userId);

    
    ProjectResponse getProject(Long projectId, Integer userId);

    
    List<ProjectResponse> listOrganizationProjects(Long organizationId, Integer userId);

    
    List<ProjectResponse> listUserProjects(Integer userId);
}
