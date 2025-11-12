package com.gradproject.taskmanager.modules.activity.repository;

import com.gradproject.taskmanager.modules.activity.domain.ActionType;
import com.gradproject.taskmanager.modules.activity.domain.ActivityLog;
import com.gradproject.taskmanager.modules.activity.domain.EntityType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    
    @Query("SELECT a FROM ActivityLog a LEFT JOIN FETCH a.user WHERE a.task.id = :taskId ORDER BY a.timestamp DESC")
    List<ActivityLog> findByTaskIdOrderByTimestampDesc(@Param("taskId") Long taskId);

    
    @Query("SELECT a FROM ActivityLog a LEFT JOIN FETCH a.user WHERE a.task.id = :taskId ORDER BY a.timestamp DESC")
    List<ActivityLog> findByTaskIdOrderByTimestampDesc(@Param("taskId") Long taskId, Pageable pageable);

    
    @Query("SELECT a FROM ActivityLog a LEFT JOIN FETCH a.user WHERE a.project.id = :projectId ORDER BY a.timestamp DESC")
    List<ActivityLog> findByProjectIdOrderByTimestampDesc(@Param("projectId") Long projectId);

    
    @Query("SELECT a FROM ActivityLog a LEFT JOIN FETCH a.user WHERE a.project.id = :projectId ORDER BY a.timestamp DESC")
    List<ActivityLog> findByProjectIdOrderByTimestampDesc(@Param("projectId") Long projectId, Pageable pageable);

    
    @Query("SELECT a FROM ActivityLog a LEFT JOIN FETCH a.user WHERE a.organization.id = :organizationId ORDER BY a.timestamp DESC")
    List<ActivityLog> findByOrganizationIdOrderByTimestampDesc(@Param("organizationId") Long organizationId);

    
    @Query("SELECT a FROM ActivityLog a LEFT JOIN FETCH a.user WHERE a.organization.id = :organizationId ORDER BY a.timestamp DESC")
    List<ActivityLog> findByOrganizationIdOrderByTimestampDesc(@Param("organizationId") Long organizationId, Pageable pageable);

    
    @Query("SELECT a FROM ActivityLog a LEFT JOIN FETCH a.user WHERE a.user.id = :userId ORDER BY a.timestamp DESC")
    List<ActivityLog> findByUserIdOrderByTimestampDesc(@Param("userId") Integer userId, Pageable pageable);

    
    @Query("SELECT a FROM ActivityLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.versionNumber ASC")
    List<ActivityLog> findByEntityTypeAndEntityIdOrderByVersionNumberAsc(
            @Param("entityType") EntityType entityType,
            @Param("entityId") Long entityId);

    
    @Query("SELECT a FROM ActivityLog a WHERE a.entityType = :entityType AND a.entityId = :entityId AND a.versionNumber <= :maxVersion ORDER BY a.versionNumber ASC")
    List<ActivityLog> findEntityStateAtVersion(
            @Param("entityType") EntityType entityType,
            @Param("entityId") Long entityId,
            @Param("maxVersion") Integer maxVersion);

    
    @Query("SELECT MAX(a.versionNumber) FROM ActivityLog a WHERE a.entityType = :entityType AND a.entityId = :entityId")
    Optional<Integer> findMaxVersionNumber(
            @Param("entityType") EntityType entityType,
            @Param("entityId") Long entityId);

    
    long countByTaskId(Long taskId);

    
    @Query("SELECT a FROM ActivityLog a WHERE a.entityType = :entityType AND a.entityId = :entityId AND a.fieldName = :fieldName ORDER BY a.versionNumber ASC")
    List<ActivityLog> findFieldHistory(
            @Param("entityType") EntityType entityType,
            @Param("entityId") Long entityId,
            @Param("fieldName") String fieldName);

    
    @Query("SELECT a FROM ActivityLog a LEFT JOIN FETCH a.user WHERE a.task.id = :taskId AND a.action = :action ORDER BY a.timestamp DESC")
    List<ActivityLog> findByTaskIdAndActionOrderByTimestampDesc(@Param("taskId") Long taskId, @Param("action") ActionType action);
}
