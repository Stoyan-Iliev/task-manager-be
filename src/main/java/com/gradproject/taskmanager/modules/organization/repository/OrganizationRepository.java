package com.gradproject.taskmanager.modules.organization.repository;

import com.gradproject.taskmanager.modules.organization.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    
    Optional<Organization> findBySlug(String slug);

    
    boolean existsBySlug(String slug);

    
    @Query("""
        SELECT o FROM Organization o
        LEFT JOIN FETCH o.members
        WHERE o.id = :id
    """)
    Optional<Organization> findByIdWithMembers(@Param("id") Long id);

    
    @Query("""
        SELECT DISTINCT o FROM Organization o
        JOIN o.members m
        WHERE m.user.id = :userId
        ORDER BY o.createdAt DESC
    """)
    List<Organization> findByUserId(@Param("userId") Integer userId);

    
    List<Organization> findByNameContainingIgnoreCase(String name);
}
