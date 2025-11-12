package com.gradproject.taskmanager.modules.git.dto.response;

import com.gradproject.taskmanager.modules.git.domain.enums.BranchStatus;

import java.time.LocalDateTime;


public record BranchResponse(
    Long id,
    Long gitIntegrationId,
    Long taskId,
    String taskKey,  
    String branchName,
    String branchRef,
    BranchStatus status,
    Boolean createdFromUi,
    String headCommitSha,
    String baseBranch,
    LocalDateTime createdAt,
    String createdByUsername,
    LocalDateTime mergedAt,
    LocalDateTime deletedAt,

    
    Integer pullRequestNumber,
    String pullRequestStatus
) {}
