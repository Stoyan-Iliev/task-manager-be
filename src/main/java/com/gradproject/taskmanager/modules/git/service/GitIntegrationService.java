package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.dto.request.CreateGitIntegrationRequest;
import com.gradproject.taskmanager.modules.git.dto.request.UpdateGitIntegrationRequest;
import com.gradproject.taskmanager.modules.git.dto.response.GitIntegrationResponse;

import java.util.List;


public interface GitIntegrationService {

    
    GitIntegrationResponse createIntegration(Long organizationId, CreateGitIntegrationRequest request, Integer userId);

    
    GitIntegrationResponse updateIntegration(Long integrationId, UpdateGitIntegrationRequest request, Integer userId);

    
    void deleteIntegration(Long integrationId, Integer userId);

    
    GitIntegrationResponse getIntegration(Long integrationId, Integer userId);

    
    List<GitIntegrationResponse> listOrganizationIntegrations(Long organizationId, Integer userId);

    
    List<GitIntegrationResponse> listProjectIntegrations(Long projectId, Integer userId);

    
    GitIntegrationResponse getByRepositoryUrl(Long organizationId, String repositoryUrl, Integer userId);

    
    GitIntegrationResponse syncIntegration(Long integrationId, Integer userId);

    
    boolean testConnection(Long organizationId, CreateGitIntegrationRequest request, Integer userId);
}
