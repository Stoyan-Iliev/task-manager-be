package com.gradproject.taskmanager.modules.organization.service;

import com.gradproject.taskmanager.modules.organization.dto.OrganizationRequest;
import com.gradproject.taskmanager.modules.organization.dto.OrganizationResponse;

import java.util.List;


public interface OrganizationService {

    
    OrganizationResponse createOrganization(OrganizationRequest request, Integer userId);

    
    OrganizationResponse updateOrganization(Long organizationId, OrganizationRequest request, Integer userId);

    
    void deleteOrganization(Long organizationId, Integer userId);

    
    OrganizationResponse getOrganization(Long organizationId, Integer userId);

    
    OrganizationResponse getOrganizationBySlug(String slug, Integer userId);

    
    List<OrganizationResponse> listMyOrganizations(Integer userId);
}
