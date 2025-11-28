package com.gradproject.taskmanager.modules.git.dto.response;

import com.gradproject.taskmanager.modules.git.domain.enums.BranchStatus;

import java.time.LocalDateTime;


public record BranchResponse(
    Long id,
    Long gitIntegrationId,
    Long taskId,
    String taskKey,
    String branchName,
    String branchUrl,
    BranchStatus status,
    String baseBranch,
    String headCommitSha,
    LocalDateTime createdAt,
    String createdByUsername,
    LocalDateTime mergedAt,
    LocalDateTime deletedAt
) {}
