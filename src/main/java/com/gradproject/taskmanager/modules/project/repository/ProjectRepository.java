package com.gradproject.taskmanager.modules.project.repository;

import com.gradproject.taskmanager.modules.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ProjectRepository extends JpaRepository<Project, Long> {

    
    @Query("SELECT p FROM Project p WHERE p.organization.id = :orgId AND p.key = :key")
    Optional<Project> findByOrganizationIdAndKey(@Param("orgId") Long orgId, @Param("key") String key);

    
    @Query("SELECT COUNT(p) > 0 FROM Project p WHERE p.organization.id = :orgId AND p.key = :key")
    boolean existsByOrganizationIdAndKey(@Param("orgId") Long orgId, @Param("key") String key);

    
    @Query("SELECT p FROM Project p WHERE p.organization.id = :orgId ORDER BY p.createdAt DESC")
    List<Project> findByOrganizationId(@Param("orgId") Long orgId);

    
    @Query("SELECT p FROM Project p WHERE p.organization.id = :orgId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Project> findByOrganizationIdAndNameContaining(@Param("orgId") Long orgId, @Param("name") String name);

    
    @Query("""
        SELECT DISTINCT p FROM Project p
        JOIN p.members m
        WHERE m.user.id = :userId
        ORDER BY p.createdAt DESC
    """)
    List<Project> findByUserId(@Param("userId") Integer userId);

    
    @Query(value = """
        SELECT p.*, ts_rank(to_tsvector('english', COALESCE(p.name, '') || ' ' || COALESCE(p.description, '')),
                           plainto_tsquery('english', :query)) as rank
        FROM projects p
        WHERE p.organization_id = :orgId
        AND to_tsvector('english', COALESCE(p.name, '') || ' ' || COALESCE(p.description, ''))
            @@ plainto_tsquery('english', :query)
        ORDER BY rank DESC
        """, nativeQuery = true)
    List<Project> fullTextSearch(@Param("query") String query, @Param("orgId") Long organizationId);
}
