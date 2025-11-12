package com.gradproject.taskmanager.modules.git.repository;

import com.gradproject.taskmanager.modules.git.domain.GitCommit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GitCommitRepository extends JpaRepository<GitCommit, Long> {

    Optional<GitCommit> findByGitIntegrationIdAndCommitSha(Long integrationId, String commitSha);

    List<GitCommit> findByGitIntegrationId(Long integrationId);

    Page<GitCommit> findByGitIntegrationId(Long integrationId, Pageable pageable);

    List<GitCommit> findByAuthorEmail(String authorEmail);

    @Query("SELECT gc FROM GitCommit gc JOIN GitCommitTask gct ON gc.id = gct.gitCommit.id WHERE gct.task.id = :taskId ORDER BY gc.authorDate DESC")
    List<GitCommit> findByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT gc FROM GitCommit gc JOIN GitCommitTask gct ON gc.id = gct.gitCommit.id WHERE gct.task.id = :taskId ORDER BY gc.authorDate DESC")
    Page<GitCommit> findByTaskId(@Param("taskId") Long taskId, Pageable pageable);

    @Query("SELECT gc FROM GitCommit gc WHERE gc.gitIntegration.id = :integrationId AND gc.authorDate BETWEEN :startDate AND :endDate ORDER BY gc.authorDate DESC")
    List<GitCommit> findByIntegrationAndDateRange(@Param("integrationId") Long integrationId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    boolean existsByGitIntegrationIdAndCommitSha(Long integrationId, String commitSha);

    @Query("SELECT COUNT(gc) FROM GitCommit gc WHERE gc.gitIntegration.project.id = :projectId AND gc.authorDate >= :since")
    long countByProjectSince(@Param("projectId") Long projectId, @Param("since") LocalDateTime since);

    Long countByGitIntegrationId(Long integrationId);
}
