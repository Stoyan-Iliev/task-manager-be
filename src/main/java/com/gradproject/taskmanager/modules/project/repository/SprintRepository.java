package com.gradproject.taskmanager.modules.project.repository;

import com.gradproject.taskmanager.modules.project.domain.Sprint;
import com.gradproject.taskmanager.modules.project.domain.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface SprintRepository extends JpaRepository<Sprint, Long> {

    
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId ORDER BY s.startDate DESC NULLS LAST, s.createdAt DESC")
    List<Sprint> findByProjectIdOrderByStartDateDesc(@Param("projectId") Long projectId);

    
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.status = :status ORDER BY s.startDate DESC")
    List<Sprint> findByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") SprintStatus status);

    
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.status = 'ACTIVE'")
    Optional<Sprint> findActiveByProjectId(@Param("projectId") Long projectId);

    
    @Query("SELECT COUNT(s) FROM Sprint s WHERE s.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    
    @Query("SELECT COUNT(s) > 0 FROM Sprint s WHERE s.project.id = :projectId AND s.name = :name")
    boolean existsByProjectIdAndName(@Param("projectId") Long projectId, @Param("name") String name);

    
    @Query("SELECT COUNT(s) FROM Sprint s WHERE s.project.id = :projectId AND s.status = 'ACTIVE'")
    long countActiveSprintsByProject(@Param("projectId") Long projectId);
}
