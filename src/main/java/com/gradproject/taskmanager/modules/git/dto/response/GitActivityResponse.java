package com.gradproject.taskmanager.modules.git.dto.response;

import java.util.List;


public record GitActivityResponse(
    Long taskId,
    String taskKey,

    
    List<BranchResponse> branches,
    List<CommitResponse> commits,
    List<PullRequestResponse> pullRequests,

    
    Integer totalBranches,
    Integer activeBranches,
    Integer totalCommits,
    Integer totalPullRequests,
    Integer openPullRequests,
    Integer mergedPullRequests,

    
    String latestCommitSha,
    String latestCommitMessage,
    java.time.LocalDateTime latestActivityAt
) {}
