package com.gradproject.taskmanager.modules.git.repository;

import com.gradproject.taskmanager.modules.git.domain.SmartCommitExecution;
import com.gradproject.taskmanager.modules.git.domain.enums.SmartCommitCommandType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmartCommitExecutionRepository extends JpaRepository<SmartCommitExecution, Long> {

    List<SmartCommitExecution> findByTaskId(Long taskId);

    List<SmartCommitExecution> findByGitCommitId(Long commitId);

    List<SmartCommitExecution> findByCommandType(SmartCommitCommandType commandType);

    List<SmartCommitExecution> findByExecutedTrue();

    List<SmartCommitExecution> findByExecutedFalse();

    @Query("SELECT sce FROM SmartCommitExecution sce WHERE sce.task.id = :taskId AND sce.executed = true ORDER BY sce.executedAt DESC")
    List<SmartCommitExecution> findSuccessfulExecutionsByTask(@Param("taskId") Long taskId);

    @Query("SELECT COUNT(sce) FROM SmartCommitExecution sce WHERE sce.task.project.id = :projectId AND sce.commandType = :commandType AND sce.executed = true")
    long countSuccessfulByProjectAndCommandType(@Param("projectId") Long projectId,
                                                 @Param("commandType") SmartCommitCommandType commandType);
}
