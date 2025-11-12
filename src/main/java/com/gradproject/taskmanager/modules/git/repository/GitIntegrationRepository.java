package com.gradproject.taskmanager.modules.git.repository;

import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitIntegrationRepository extends JpaRepository<GitIntegration, Long> {

    List<GitIntegration> findByOrganizationId(Long organizationId);

    List<GitIntegration> findByOrganizationIdAndProjectId(Long organizationId, Long projectId);

    List<GitIntegration> findByOrganizationIdAndProjectIdIsNull(Long organizationId);

    Optional<GitIntegration> findByOrganizationIdAndRepositoryFullName(Long organizationId, String repositoryFullName);

    List<GitIntegration> findByOrganizationIdAndIsActiveTrue(Long organizationId);

    List<GitIntegration> findByProvider(GitProvider provider);

    @Query("SELECT gi FROM GitIntegration gi WHERE gi.organization.id = :orgId AND gi.isActive = true")
    List<GitIntegration> findActiveByOrganization(@Param("orgId") Long organizationId);

    @Query("SELECT gi FROM GitIntegration gi WHERE gi.project.id = :projectId AND gi.isActive = true")
    List<GitIntegration> findActiveByProject(@Param("projectId") Long projectId);

    boolean existsByOrganizationIdAndRepositoryFullName(Long organizationId, String repositoryFullName);

    List<GitIntegration> findByProjectId(Long projectId);

    Optional<GitIntegration> findByOrganizationIdAndRepositoryUrl(Long organizationId, String repositoryUrl);

    boolean existsByOrganizationIdAndRepositoryUrl(Long organizationId, String repositoryUrl);

    Optional<GitIntegration> findByRepositoryUrl(String repositoryUrl);
}
