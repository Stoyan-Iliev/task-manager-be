package com.gradproject.taskmanager.modules.organization.repository;

import com.gradproject.taskmanager.modules.organization.domain.OrganizationMember;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    
    @Query("""
        SELECT m FROM OrganizationMember m
        JOIN FETCH m.user
        WHERE m.organization.id = :organizationId
        ORDER BY m.joinedAt DESC
    """)
    List<OrganizationMember> findByOrganizationId(@Param("organizationId") Long organizationId);

    
    @Query("""
        SELECT m FROM OrganizationMember m
        JOIN FETCH m.organization
        WHERE m.user.id = :userId
        ORDER BY m.joinedAt DESC
    """)
    List<OrganizationMember> findByUserId(@Param("userId") Integer userId);

    
    @Query("""
        SELECT m FROM OrganizationMember m
        WHERE m.user.id = :userId AND m.organization.id = :organizationId
    """)
    Optional<OrganizationMember> findByUserIdAndOrganizationId(
        @Param("userId") Integer userId,
        @Param("organizationId") Long organizationId
    );

    
    boolean existsByUserIdAndOrganizationId(Integer userId, Long organizationId);

    
    long countByOrganizationId(Long organizationId);

    
    List<OrganizationMember> findByOrganizationIdAndRole(Long organizationId, OrganizationRole role);

    
    void deleteByUserIdAndOrganizationId(Integer userId, Long organizationId);
}
