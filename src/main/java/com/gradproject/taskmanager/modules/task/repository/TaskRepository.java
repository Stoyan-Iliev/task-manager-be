package com.gradproject.taskmanager.modules.task.repository;

import com.gradproject.taskmanager.modules.task.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignee " +
           "LEFT JOIN FETCH t.reporter " +
           "WHERE t.id = :id")
    Optional<Task> findByIdWithAssociations(@Param("id") Long id);


    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.project p " +
           "JOIN FETCH t.status s " +
           "LEFT JOIN FETCH t.assignee " +
           "LEFT JOIN FETCH t.reporter " +
           "WHERE t.organization.id = :orgId AND t.key = :key")
    Optional<Task> findByOrganizationIdAndKey(@Param("orgId") Long orgId, @Param("key") String key);

    
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignee " +
           "LEFT JOIN FETCH t.reporter " +
           "JOIN FETCH t.status " +
           "WHERE t.project.id = :projectId " +
           "AND (:statusId IS NULL OR t.status.id = :statusId) " +
           "AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId) " +
           "AND (:sprintId IS NULL OR t.sprint.id = :sprintId) " +
           "ORDER BY t.createdAt DESC")
    List<Task> findByProjectWithFilters(
        @Param("projectId") Long projectId,
        @Param("statusId") Long statusId,
        @Param("assigneeId") Integer assigneeId,
        @Param("sprintId") Long sprintId
    );

    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status.id = :statusId")
    long countByProjectIdAndStatusId(@Param("projectId") Long projectId, @Param("statusId") Long statusId);

    
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignee " +
           "JOIN FETCH t.status " +
           "WHERE t.parentTask.id = :parentTaskId " +
           "ORDER BY t.createdAt ASC")
    List<Task> findSubtasks(@Param("parentTaskId") Long parentTaskId);

    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.parentTask.id = :parentTaskId")
    long countSubtasks(@Param("parentTaskId") Long parentTaskId);

    
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.status s " +
           "WHERE t.project.id = :projectId " +
           "AND t.dueDate < :today " +
           "AND s.category != 'DONE' " +
           "ORDER BY t.dueDate ASC")
    List<Task> findOverdueTasks(@Param("projectId") Long projectId, @Param("today") LocalDate today);

    
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.project p " +
           "JOIN FETCH t.status s " +
           "WHERE t.assignee.id = :userId " +
           "AND t.organization.id = :orgId " +
           "AND s.category != 'DONE' " +
           "ORDER BY CASE t.priority " +
           "  WHEN 'HIGHEST' THEN 1 " +
           "  WHEN 'HIGH' THEN 2 " +
           "  WHEN 'MEDIUM' THEN 3 " +
           "  WHEN 'LOW' THEN 4 " +
           "  WHEN 'LOWEST' THEN 5 " +
           "END, t.createdAt DESC")
    List<Task> findMyOpenTasks(@Param("userId") Integer userId, @Param("orgId") Long orgId);

    
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.status " +
           "LEFT JOIN FETCH t.assignee " +
           "WHERE t.sprint.id = :sprintId " +
           "ORDER BY t.createdAt DESC")
    List<Task> findBySprintId(@Param("sprintId") Long sprintId);

    
    @Query("SELECT s.category, COUNT(t) FROM Task t " +
           "JOIN t.status s " +
           "WHERE t.sprint.id = :sprintId " +
           "GROUP BY s.category")
    List<Object[]> countBySprintAndStatusCategory(@Param("sprintId") Long sprintId);

    
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.status s " +
           "LEFT JOIN FETCH t.assignee " +
           "WHERE t.project.id = :projectId " +
           "AND t.sprint IS NULL " +
           "ORDER BY CASE t.priority " +
           "  WHEN 'HIGHEST' THEN 1 " +
           "  WHEN 'HIGH' THEN 2 " +
           "  WHEN 'MEDIUM' THEN 3 " +
           "  WHEN 'LOW' THEN 4 " +
           "  WHEN 'LOWEST' THEN 5 " +
           "END, t.createdAt DESC")
    List<Task> findBacklogTasks(@Param("projectId") Long projectId);

    
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.status s " +
           "WHERE t.sprint.id = :sprintId " +
           "AND s.category != 'DONE' " +
           "ORDER BY t.createdAt DESC")
    List<Task> findIncompleteTasksBySprintId(@Param("sprintId") Long sprintId);

    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.sprint.id = :sprintId")
    int countBySprintId(@Param("sprintId") Long sprintId);

    
    @Query("SELECT COUNT(t) FROM Task t " +
           "JOIN t.status s " +
           "WHERE t.sprint.id = :sprintId " +
           "AND s.category = 'DONE'")
    int countCompletedBySprintId(@Param("sprintId") Long sprintId);

    
    @Query("SELECT COALESCE(SUM(t.estimatedHours), 0) FROM Task t WHERE t.sprint.id = :sprintId")
    Double sumPointsBySprintId(@Param("sprintId") Long sprintId);

    
    @Query("SELECT COALESCE(SUM(t.estimatedHours), 0) FROM Task t " +
           "JOIN t.status s " +
           "WHERE t.sprint.id = :sprintId " +
           "AND s.category = 'DONE'")
    Double sumCompletedPointsBySprintId(@Param("sprintId") Long sprintId);

    
    @Query(value = """
        SELECT t.*, ts_rank(to_tsvector('english', COALESCE(t.title, '') || ' ' || COALESCE(t.description, '')),
                           plainto_tsquery('english', :query)) as rank
        FROM tasks t
        WHERE t.organization_id = :orgId
        AND to_tsvector('english', COALESCE(t.title, '') || ' ' || COALESCE(t.description, ''))
            @@ plainto_tsquery('english', :query)
        ORDER BY rank DESC
        """, nativeQuery = true)
    List<Task> fullTextSearch(@Param("query") String query, @Param("orgId") Long organizationId);
}
