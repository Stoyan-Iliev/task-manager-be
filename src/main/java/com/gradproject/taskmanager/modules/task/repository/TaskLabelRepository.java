package com.gradproject.taskmanager.modules.task.repository;

import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface TaskLabelRepository extends JpaRepository<TaskLabel, Long> {

    
    List<TaskLabel> findByTaskId(Long taskId);

    
    List<TaskLabel> findByLabelId(Long labelId);

    
    Optional<TaskLabel> findByTaskIdAndLabelId(Long taskId, Long labelId);

    
    boolean existsByTaskIdAndLabelId(Long taskId, Long labelId);

    
    @Modifying
    @Query("DELETE FROM TaskLabel tl WHERE tl.task.id = :taskId AND tl.label.id = :labelId")
    void deleteByTaskIdAndLabelId(@Param("taskId") Long taskId, @Param("labelId") Long labelId);

    
    @Query("SELECT DISTINCT tl.task FROM TaskLabel tl WHERE tl.label.id IN :labelIds")
    List<Task> findTasksByLabelIds(@Param("labelIds") List<Long> labelIds);

    
    @Query("""
        SELECT tl.task FROM TaskLabel tl
        WHERE tl.label.id IN :labelIds
        GROUP BY tl.task
        HAVING COUNT(DISTINCT tl.label.id) = :labelCount
        """)
    List<Task> findTasksWithAllLabels(@Param("labelIds") List<Long> labelIds, @Param("labelCount") long labelCount);

    
    long countByTaskId(Long taskId);

    
    @Modifying
    @Query("DELETE FROM TaskLabel tl WHERE tl.task.id = :taskId")
    void deleteAllByTaskId(@Param("taskId") Long taskId);
}
