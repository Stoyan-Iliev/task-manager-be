package com.gradproject.taskmanager.modules.git.repository;

import com.gradproject.taskmanager.modules.git.domain.GitDeployment;
import com.gradproject.taskmanager.modules.git.domain.enums.DeploymentEnvironment;
import com.gradproject.taskmanager.modules.git.domain.enums.DeploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GitDeploymentRepository extends JpaRepository<GitDeployment, Long> {

    List<GitDeployment> findByGitIntegrationId(Long integrationId);

    Page<GitDeployment> findByGitIntegrationId(Long integrationId, Pageable pageable);

    List<GitDeployment> findByCommitSha(String commitSha);

    List<GitDeployment> findByEnvironment(DeploymentEnvironment environment);

    List<GitDeployment> findByGitIntegrationIdAndEnvironment(Long integrationId, DeploymentEnvironment environment);

    @Query("SELECT gd FROM GitDeployment gd WHERE gd.gitIntegration.id = :integrationId AND gd.environment = :environment AND gd.completedAt IS NOT NULL ORDER BY gd.completedAt DESC")
    List<GitDeployment> findSuccessfulDeploymentsByIntegrationAndEnvironment(@Param("integrationId") Long integrationId,
                                                                              @Param("environment") DeploymentEnvironment environment,
                                                                              Pageable pageable);

    @Query("SELECT gd FROM GitDeployment gd WHERE gd.gitIntegration.id = :integrationId AND gd.startedAt BETWEEN :startDate AND :endDate")
    List<GitDeployment> findByIntegrationAndDateRange(@Param("integrationId") Long integrationId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(gd) FROM GitDeployment gd WHERE gd.gitIntegration.project.id = :projectId AND gd.deploymentStatus = :status AND gd.environment = :environment")
    long countByProjectAndStatusAndEnvironment(@Param("projectId") Long projectId,
                                                @Param("status") DeploymentStatus status,
                                                @Param("environment") DeploymentEnvironment environment);
}
