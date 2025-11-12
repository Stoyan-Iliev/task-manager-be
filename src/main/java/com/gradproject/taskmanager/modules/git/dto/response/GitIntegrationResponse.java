package com.gradproject.taskmanager.modules.git.dto.response;

import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;

import java.time.LocalDateTime;


public record GitIntegrationResponse(
    Long id,
    Long organizationId,
    Long projectId,
    GitProvider provider,
    String repositoryUrl,
    String repositoryOwner,
    String repositoryName,
    String repositoryFullName,

    
    String webhookId,
    String webhookUrl,
    Boolean webhookActive,

    
    Boolean autoLinkEnabled,
    Boolean smartCommitsEnabled,
    Boolean autoCloseOnMerge,
    String branchPrefix,

    
    Boolean isActive,
    LocalDateTime lastSyncAt,

    
    LocalDateTime createdAt,
    String createdByUsername,

    
    Long branchCount,
    Long commitCount,
    Long pullRequestCount
) {}
