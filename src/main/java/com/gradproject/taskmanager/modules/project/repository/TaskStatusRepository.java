package com.gradproject.taskmanager.modules.project.repository;

import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface TaskStatusRepository extends JpaRepository<TaskStatus, Long> {

    
    @Query("SELECT ts FROM TaskStatus ts WHERE ts.project.id = :projectId ORDER BY ts.orderIndex ASC")
    List<TaskStatus> findByProjectIdOrderByOrderIndexAsc(@Param("projectId") Long projectId);

    
    @Query("SELECT ts FROM TaskStatus ts WHERE ts.project.id = :projectId AND ts.isDefault = true")
    Optional<TaskStatus> findDefaultByProjectId(@Param("projectId") Long projectId);

    
    @Query("SELECT COUNT(ts) FROM TaskStatus ts WHERE ts.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    
    @Query("SELECT COUNT(ts) > 0 FROM TaskStatus ts WHERE ts.project.id = :projectId AND ts.name = :name AND ts.id != :excludeId")
    boolean existsByProjectIdAndNameExcludingId(@Param("projectId") Long projectId, @Param("name") String name, @Param("excludeId") Long excludeId);

    
    @Query("SELECT COUNT(ts) > 0 FROM TaskStatus ts WHERE ts.project.id = :projectId AND ts.name = :name")
    boolean existsByProjectIdAndName(@Param("projectId") Long projectId, @Param("name") String name);

    
    @Query("SELECT COALESCE(MAX(ts.orderIndex), -1) FROM TaskStatus ts WHERE ts.project.id = :projectId")
    Integer findMaxOrderIndexByProjectId(@Param("projectId") Long projectId);
}
