package com.gradproject.taskmanager.modules.release.repository;

import com.gradproject.taskmanager.modules.release.domain.ReleaseTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ReleaseTaskRepository extends JpaRepository<ReleaseTask, Long> {

    
    @Query("SELECT rt FROM ReleaseTask rt WHERE rt.release.id = :releaseId ORDER BY rt.addedAt DESC")
    List<ReleaseTask> findByReleaseId(@Param("releaseId") Long releaseId);

    
    @Query("SELECT rt FROM ReleaseTask rt WHERE rt.task.id = :taskId ORDER BY rt.addedAt DESC")
    List<ReleaseTask> findByTaskId(@Param("taskId") Long taskId);

    
    @Query("SELECT rt FROM ReleaseTask rt WHERE rt.release.id = :releaseId AND rt.task.id = :taskId")
    Optional<ReleaseTask> findByReleaseIdAndTaskId(@Param("releaseId") Long releaseId, @Param("taskId") Long taskId);

    
    @Query("SELECT COUNT(rt) > 0 FROM ReleaseTask rt WHERE rt.release.id = :releaseId AND rt.task.id = :taskId")
    boolean existsByReleaseIdAndTaskId(@Param("releaseId") Long releaseId, @Param("taskId") Long taskId);

    
    @Query("SELECT COUNT(rt) FROM ReleaseTask rt WHERE rt.release.id = :releaseId")
    long countByReleaseId(@Param("releaseId") Long releaseId);

    
    @Query("SELECT COUNT(rt) FROM ReleaseTask rt JOIN rt.task t JOIN t.status s WHERE rt.release.id = :releaseId AND s.category = 'DONE'")
    long countCompletedByReleaseId(@Param("releaseId") Long releaseId);
}
