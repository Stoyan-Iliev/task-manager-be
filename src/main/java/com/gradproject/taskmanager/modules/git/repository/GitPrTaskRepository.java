package com.gradproject.taskmanager.modules.git.repository;

import com.gradproject.taskmanager.modules.git.domain.GitPrTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitPrTaskRepository extends JpaRepository<GitPrTask, Long> {

    List<GitPrTask> findByTaskId(Long taskId);

    List<GitPrTask> findByGitPullRequestId(Long prId);

    Optional<GitPrTask> findByGitPullRequestIdAndTaskId(Long prId, Long taskId);

    List<GitPrTask> findByClosesTaskTrue();

    boolean existsByGitPullRequestIdAndTaskId(Long prId, Long taskId);
}
