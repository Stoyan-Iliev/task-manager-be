package com.gradproject.taskmanager.modules.git.repository;

import com.gradproject.taskmanager.modules.git.domain.GitBranch;
import com.gradproject.taskmanager.modules.git.domain.enums.BranchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitBranchRepository extends JpaRepository<GitBranch, Long> {

    List<GitBranch> findByTaskId(Long taskId);

    List<GitBranch> findByTaskIdAndStatus(Long taskId, BranchStatus status);

    List<GitBranch> findByGitIntegrationId(Long integrationId);

    Optional<GitBranch> findByGitIntegrationIdAndBranchName(Long integrationId, String branchName);

    @Query("SELECT gb FROM GitBranch gb WHERE gb.task.id = :taskId AND gb.status = 'ACTIVE'")
    List<GitBranch> findActiveBranchesByTask(@Param("taskId") Long taskId);

    @Query("SELECT gb FROM GitBranch gb WHERE gb.task.project.id = :projectId AND gb.status = :status")
    List<GitBranch> findByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") BranchStatus status);

    boolean existsByGitIntegrationIdAndBranchName(Long integrationId, String branchName);

    Long countByGitIntegrationId(Long integrationId);
}
