package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.git.domain.GitBranch;
import com.gradproject.taskmanager.modules.git.domain.GitCommit;
import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.domain.GitPullRequest;
import com.gradproject.taskmanager.modules.git.dto.response.BranchResponse;
import com.gradproject.taskmanager.modules.git.dto.response.CommitResponse;
import com.gradproject.taskmanager.modules.git.dto.response.GitIntegrationResponse;
import com.gradproject.taskmanager.modules.git.dto.response.PullRequestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;


@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface GitIntegrationMapper {

    
    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "branchCount", expression = "java(countBranches(integration))")
    @Mapping(target = "commitCount", expression = "java(countCommits(integration))")
    @Mapping(target = "pullRequestCount", expression = "java(countPullRequests(integration))")
    GitIntegrationResponse toResponse(GitIntegration integration);

    
    @Mapping(target = "gitIntegrationId", source = "gitIntegration.id")
    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "taskKey", source = "task.key")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "pullRequestNumber", expression = "java(extractPrNumber(branch))")
    @Mapping(target = "pullRequestStatus", expression = "java(extractPrStatus(branch))")
    BranchResponse toBranchResponse(GitBranch branch);

    
    @Mapping(target = "gitIntegrationId", source = "gitIntegration.id")
    @Mapping(target = "linkedTaskKeys", expression = "java(extractLinkedTaskKeys(commit))")
    @Mapping(target = "smartCommands", expression = "java(extractSmartCommands(commit))")
    CommitResponse toCommitResponse(GitCommit commit);

    
    @Mapping(target = "gitIntegrationId", source = "gitIntegration.id")
    @Mapping(target = "gitBranchId", source = "gitBranch.id")
    @Mapping(target = "linkedTaskKeys", expression = "java(extractPrLinkedTaskKeys(pr))")
    @Mapping(target = "reviewers", expression = "java(extractReviewers(pr))")
    @Mapping(target = "checks", expression = "java(extractChecks(pr))")
    PullRequestResponse toPullRequestResponse(GitPullRequest pr);

    
    

    default Long countBranches(GitIntegration integration) {
        
        return null;
    }

    default Long countCommits(GitIntegration integration) {
        
        return null;
    }

    default Long countPullRequests(GitIntegration integration) {
        
        return null;
    }

    default Integer extractPrNumber(GitBranch branch) {
        
        return null;
    }

    default String extractPrStatus(GitBranch branch) {
        
        return null;
    }

    default java.util.List<String> extractLinkedTaskKeys(GitCommit commit) {
        
        
        return java.util.List.of();
    }

    default java.util.List<String> extractSmartCommands(GitCommit commit) {
        
        
        return java.util.List.of();
    }

    default java.util.List<String> extractPrLinkedTaskKeys(GitPullRequest pr) {
        
        
        return java.util.List.of();
    }

    default java.util.List<PullRequestResponse.ReviewerInfo> extractReviewers(GitPullRequest pr) {
        
        
        return java.util.List.of();
    }

    default java.util.List<PullRequestResponse.CheckInfo> extractChecks(GitPullRequest pr) {
        
        
        return java.util.List.of();
    }
}
