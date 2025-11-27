package com.gradproject.taskmanager.modules.git.repository;

import com.gradproject.taskmanager.modules.git.domain.GitPullRequest;
import com.gradproject.taskmanager.modules.git.domain.enums.PullRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitPullRequestRepository extends JpaRepository<GitPullRequest, Long> {

    Optional<GitPullRequest> findByGitIntegrationIdAndPrNumber(Long integrationId, Integer prNumber);

    List<GitPullRequest> findByGitIntegrationId(Long integrationId);

    List<GitPullRequest> findByGitBranchId(Long branchId);

    List<GitPullRequest> findByStatus(PullRequestStatus status);

    @Query("SELECT gpr FROM GitPullRequest gpr JOIN GitPrTask gpt ON gpr.id = gpt.gitPullRequest.id WHERE gpt.task.id = :taskId ORDER BY gpr.createdAt DESC")
    List<GitPullRequest> findByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT gpr FROM GitPullRequest gpr WHERE gpr.gitIntegration.id = :integrationId AND gpr.status IN :statuses")
    List<GitPullRequest> findByIntegrationAndStatuses(@Param("integrationId") Long integrationId,
                                                       @Param("statuses") List<PullRequestStatus> statuses);

    boolean existsByGitIntegrationIdAndPrNumber(Long integrationId, Integer prNumber);

    @Query("SELECT COUNT(gpr) FROM GitPullRequest gpr WHERE gpr.gitIntegration.project.id = :projectId AND gpr.status = :status")
    long countByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") PullRequestStatus status);

    Long countByGitIntegrationId(Long integrationId);

    /**
     * Find all PRs for a branch, ordered by relevance:
     * - OPEN/DRAFT PRs first (most relevant for active branches)
     * - Then by most recently updated
     * Use findTopByGitIntegrationIdAndSourceBranch() to get the most relevant one.
     */
    @Query("""
        SELECT gpr FROM GitPullRequest gpr
        WHERE gpr.gitIntegration.id = :integrationId AND gpr.sourceBranch = :sourceBranch
        ORDER BY
            CASE gpr.status
                WHEN com.gradproject.taskmanager.modules.git.domain.enums.PullRequestStatus.OPEN THEN 0
                WHEN com.gradproject.taskmanager.modules.git.domain.enums.PullRequestStatus.DRAFT THEN 1
                WHEN com.gradproject.taskmanager.modules.git.domain.enums.PullRequestStatus.MERGED THEN 2
                WHEN com.gradproject.taskmanager.modules.git.domain.enums.PullRequestStatus.CLOSED THEN 3
            END,
            gpr.updatedAt DESC NULLS LAST,
            gpr.createdAt DESC
        """)
    List<GitPullRequest> findByGitIntegrationIdAndSourceBranchOrderByRelevance(
            @Param("integrationId") Long integrationId,
            @Param("sourceBranch") String sourceBranch);
}
