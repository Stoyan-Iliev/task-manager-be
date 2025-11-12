package com.gradproject.taskmanager.modules.project.repository;

import com.gradproject.taskmanager.modules.project.domain.ProjectMember;
import com.gradproject.taskmanager.modules.project.domain.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    
    @Query("SELECT pm FROM ProjectMember pm WHERE pm.user.id = :userId AND pm.project.id = :projectId")
    Optional<ProjectMember> findByUserIdAndProjectId(@Param("userId") Integer userId, @Param("projectId") Long projectId);

    
    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId ORDER BY pm.addedAt ASC")
    List<ProjectMember> findByProjectId(@Param("projectId") Long projectId);

    
    @Query("SELECT pm FROM ProjectMember pm WHERE pm.user.id = :userId ORDER BY pm.addedAt DESC")
    List<ProjectMember> findByUserId(@Param("userId") Integer userId);

    
    @Query("SELECT COUNT(pm) FROM ProjectMember pm WHERE pm.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    
    @Query("SELECT COUNT(pm) FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.role = :role")
    long countByProjectIdAndRole(@Param("projectId") Long projectId, @Param("role") ProjectRole role);

    
    @Query("SELECT COUNT(pm) > 0 FROM ProjectMember pm WHERE pm.user.id = :userId AND pm.project.id = :projectId")
    boolean existsByUserIdAndProjectId(@Param("userId") Integer userId, @Param("projectId") Long projectId);
}
