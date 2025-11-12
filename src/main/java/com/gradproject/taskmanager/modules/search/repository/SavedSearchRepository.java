package com.gradproject.taskmanager.modules.search.repository;

import com.gradproject.taskmanager.modules.search.domain.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    List<SavedSearch> findByUserIdAndOrganizationId(Integer userId, Long organizationId);

    List<SavedSearch> findByOrganizationIdAndIsSharedTrue(Long organizationId);

    Optional<SavedSearch> findByIdAndUserId(Long id, Integer userId);

    @Query("SELECT ss FROM SavedSearch ss WHERE ss.organization.id = :orgId AND (ss.user.id = :userId OR ss.isShared = true)")
    List<SavedSearch> findAvailableSearches(@Param("orgId") Long organizationId, @Param("userId") Integer userId);

    boolean existsByIdAndUserId(Long id, Integer userId);
}
