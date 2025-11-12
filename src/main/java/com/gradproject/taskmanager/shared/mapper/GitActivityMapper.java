package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.git.domain.GitBranch;
import com.gradproject.taskmanager.modules.git.domain.GitCommit;
import com.gradproject.taskmanager.modules.git.domain.GitPullRequest;
import com.gradproject.taskmanager.modules.git.dto.response.BranchResponse;
import com.gradproject.taskmanager.modules.git.dto.response.CommitResponse;
import com.gradproject.taskmanager.modules.git.dto.response.GitActivityResponse;
import com.gradproject.taskmanager.modules.git.dto.response.PullRequestResponse;
import com.gradproject.taskmanager.modules.task.domain.Task;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;


@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = {GitIntegrationMapper.class}
)
public abstract class GitActivityMapper {

    @Autowired
    protected GitIntegrationMapper gitIntegrationMapper;

    
    public GitActivityResponse toActivityResponse(
        Task task,
        List<GitBranch> branches,
        List<GitCommit> commits,
        List<GitPullRequest> pullRequests
    ) {
        
        List<BranchResponse> branchResponses = branches != null ?
            branches.stream().map(gitIntegrationMapper::toBranchResponse).toList() :
            List.of();

        List<CommitResponse> commitResponses = commits != null ?
            commits.stream().map(gitIntegrationMapper::toCommitResponse).toList() :
            List.of();

        List<PullRequestResponse> prResponses = pullRequests != null ?
            pullRequests.stream().map(gitIntegrationMapper::toPullRequestResponse).toList() :
            List.of();

        
        int totalBranches = branchResponses.size();
        int activeBranches = (int) branchResponses.stream()
            .filter(b -> "ACTIVE".equals(b.status()))
            .count();

        int totalCommits = commitResponses.size();

        int totalPullRequests = prResponses.size();
        int openPullRequests = (int) prResponses.stream()
            .filter(pr -> "OPEN".equals(pr.status()))
            .count();
        int mergedPullRequests = (int) prResponses.stream()
            .filter(pr -> "MERGED".equals(pr.status()))
            .count();

        
        String latestCommitSha = null;
        String latestCommitMessage = null;
        LocalDateTime latestActivityAt = null;

        if (!commitResponses.isEmpty()) {
            CommitResponse latestCommit = commitResponses.stream()
                .max(Comparator.comparing(CommitResponse::authorDate))
                .orElse(null);

            if (latestCommit != null) {
                latestCommitSha = latestCommit.commitSha();
                latestCommitMessage = latestCommit.message();
                latestActivityAt = latestCommit.authorDate();
            }
        }

        return new GitActivityResponse(
            task.getId(),
            task.getKey(),
            branchResponses,
            commitResponses,
            prResponses,
            totalBranches,
            activeBranches,
            totalCommits,
            totalPullRequests,
            openPullRequests,
            mergedPullRequests,
            latestCommitSha,
            latestCommitMessage,
            latestActivityAt
        );
    }
}
