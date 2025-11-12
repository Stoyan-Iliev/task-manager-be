package com.gradproject.taskmanager.modules.git.repository;

import com.gradproject.taskmanager.modules.git.domain.GitCommitTask;
import com.gradproject.taskmanager.modules.git.domain.enums.LinkMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitCommitTaskRepository extends JpaRepository<GitCommitTask, Long> {

    List<GitCommitTask> findByTaskId(Long taskId);

    List<GitCommitTask> findByGitCommitId(Long commitId);

    Optional<GitCommitTask> findByGitCommitIdAndTaskId(Long commitId, Long taskId);

    List<GitCommitTask> findByLinkMethod(LinkMethod linkMethod);

    boolean existsByGitCommitIdAndTaskId(Long commitId, Long taskId);

    long countByTaskId(Long taskId);
}
