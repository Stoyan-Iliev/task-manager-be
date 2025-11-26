package com.gradproject.taskmanager.modules.task.repository;

import com.gradproject.taskmanager.modules.task.domain.WorkLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

    /**
     * Find all work logs for a task, ordered by work date descending.
     */
    List<WorkLog> findByTaskIdOrderByWorkDateDescCreatedAtDesc(Long taskId);

    /**
     * Find all work logs for a task with pagination.
     */
    Page<WorkLog> findByTaskIdOrderByWorkDateDescCreatedAtDesc(Long taskId, Pageable pageable);

    /**
     * Find work logs by author, ordered by work date descending.
     */
    List<WorkLog> findByAuthorIdOrderByWorkDateDescCreatedAtDesc(Integer authorId);

    /**
     * Find work logs by author with pagination.
     */
    Page<WorkLog> findByAuthorIdOrderByWorkDateDescCreatedAtDesc(Integer authorId, Pageable pageable);

    /**
     * Find work logs for a task within a date range.
     */
    @Query("SELECT w FROM WorkLog w WHERE w.task.id = :taskId " +
           "AND w.workDate BETWEEN :startDate AND :endDate " +
           "ORDER BY w.workDate DESC, w.createdAt DESC")
    List<WorkLog> findByTaskIdAndDateRange(
            @Param("taskId") Long taskId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find work logs for an author within a date range.
     */
    @Query("SELECT w FROM WorkLog w WHERE w.author.id = :authorId " +
           "AND w.workDate BETWEEN :startDate AND :endDate " +
           "ORDER BY w.workDate DESC, w.createdAt DESC")
    List<WorkLog> findByAuthorIdAndDateRange(
            @Param("authorId") Integer authorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total time spent on a task (in minutes).
     */
    @Query("SELECT COALESCE(SUM(w.timeSpentMinutes), 0) FROM WorkLog w WHERE w.task.id = :taskId")
    Integer getTotalTimeSpentForTask(@Param("taskId") Long taskId);

    /**
     * Count work log entries for a task.
     */
    long countByTaskId(Long taskId);

    /**
     * Delete all work logs for a task.
     */
    void deleteByTaskId(Long taskId);

    /**
     * Check if work log belongs to author.
     */
    boolean existsByIdAndAuthorId(Long workLogId, Integer authorId);

    /**
     * Find work logs for multiple tasks (useful for batch operations).
     */
    @Query("SELECT w FROM WorkLog w WHERE w.task.id IN :taskIds ORDER BY w.workDate DESC, w.createdAt DESC")
    List<WorkLog> findByTaskIdIn(@Param("taskIds") List<Long> taskIds);

    /**
     * Calculate total time spent by author on a specific date.
     */
    @Query("SELECT COALESCE(SUM(w.timeSpentMinutes), 0) FROM WorkLog w " +
           "WHERE w.author.id = :authorId AND w.workDate = :workDate")
    Integer getTotalTimeSpentByAuthorOnDate(
            @Param("authorId") Integer authorId,
            @Param("workDate") LocalDate workDate);

    /**
     * Find work logs for tasks in a project.
     */
    @Query("SELECT w FROM WorkLog w WHERE w.task.project.id = :projectId " +
           "ORDER BY w.workDate DESC, w.createdAt DESC")
    List<WorkLog> findByProjectId(@Param("projectId") Long projectId);

    /**
     * Calculate total time spent on tasks in a project.
     */
    @Query("SELECT COALESCE(SUM(w.timeSpentMinutes), 0) FROM WorkLog w WHERE w.task.project.id = :projectId")
    Integer getTotalTimeSpentForProject(@Param("projectId") Long projectId);
}
