package com.gradproject.taskmanager.modules.git.dto.response;

import com.gradproject.taskmanager.modules.git.domain.enums.ChecksStatus;
import com.gradproject.taskmanager.modules.git.domain.enums.PullRequestStatus;

import java.time.LocalDateTime;
import java.util.List;


public record PullRequestResponse(
    Long id,
    Long gitIntegrationId,
    Long gitBranchId,
    Integer prNumber,
    String prTitle,
    String prDescription,
    String prUrl,
    PullRequestStatus status,

    
    String sourceBranch,
    String targetBranch,
    String headCommitSha,

    
    String authorUsername,
    String authorName,
    String authorEmail,

    
    List<ReviewerInfo> reviewers,
    Integer approvalsCount,
    Integer requiredApprovals,
    Boolean approved,  

    
    ChecksStatus checksStatus,
    Integer checksCount,
    Integer checksPassed,
    List<CheckInfo> checks,
    Boolean allChecksPassed,  

    
    Boolean mergeable,
    Boolean merged,
    LocalDateTime mergedAt,
    String mergedBy,
    String mergeCommitSha,

    
    List<String> linkedTaskKeys,
    Boolean closesTask,

    
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime closedAt
) {
    
    public record ReviewerInfo(
        String username,
        String name,
        String status,  
        LocalDateTime reviewedAt
    ) {}

    public record CheckInfo(
        String name,
        String status,  
        String url
    ) {}

    
    public PullRequestResponse {
        if (approved == null && approvalsCount != null && requiredApprovals != null) {
            approved = approvalsCount >= requiredApprovals;
        }
        if (allChecksPassed == null && checksCount != null && checksPassed != null) {
            allChecksPassed = checksCount.equals(checksPassed);
        }
    }
}
