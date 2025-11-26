package com.gradproject.taskmanager.modules.task.repository;

import com.gradproject.taskmanager.modules.task.domain.TaskWatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface TaskWatcherRepository extends JpaRepository<TaskWatcher, Long> {


    List<TaskWatcher> findByTaskId(Long taskId);

    @Query("SELECT tw FROM TaskWatcher tw JOIN FETCH tw.user WHERE tw.task.id = :taskId")
    List<TaskWatcher> findByTaskIdWithUser(@Param("taskId") Long taskId);

    
    List<TaskWatcher> findByUserId(Integer userId);

    
    @Query("SELECT tw FROM TaskWatcher tw WHERE tw.task.id = :taskId AND tw.user.id = :userId")
    Optional<TaskWatcher> findByTaskIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Integer userId);

    
    boolean existsByTaskIdAndUserId(Long taskId, Integer userId);

    
    @Modifying
    @Query("DELETE FROM TaskWatcher tw WHERE tw.task.id = :taskId AND tw.user.id = :userId")
    void deleteByTaskIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Integer userId);

    
    @Modifying
    @Query("DELETE FROM TaskWatcher tw WHERE tw.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);

    
    @Query("SELECT COUNT(tw) FROM TaskWatcher tw WHERE tw.task.id = :taskId")
    int countByTaskId(@Param("taskId") Long taskId);

    
    @Query("SELECT tw.task.id, COUNT(tw) FROM TaskWatcher tw WHERE tw.task.id IN :taskIds GROUP BY tw.task.id")
    List<Object[]> countWatchersByTaskIds(@Param("taskIds") List<Long> taskIds);

    
    @Query("SELECT tw FROM TaskWatcher tw WHERE tw.user.id = :userId ORDER BY tw.addedAt DESC")
    Page<TaskWatcher> findByUserIdOrderByAddedAtDesc(@Param("userId") Integer userId, Pageable pageable);

    
    @Query("SELECT tw FROM TaskWatcher tw WHERE tw.task.project.id = :projectId AND tw.user.id = :userId")
    List<TaskWatcher> findByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Integer userId);

    
    long countByUserId(Integer userId);
}
