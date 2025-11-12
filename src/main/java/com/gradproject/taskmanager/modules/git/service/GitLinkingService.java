package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.GitBranch;
import com.gradproject.taskmanager.modules.git.domain.GitCommit;
import com.gradproject.taskmanager.modules.git.domain.GitPullRequest;
import com.gradproject.taskmanager.modules.task.domain.Task;

import java.util.List;


public interface GitLinkingService {

    
    Task linkBranchToTask(GitBranch branch);

    
    List<Task> linkCommitToTasks(GitCommit commit);

    
    List<Task> linkPullRequestToTasks(GitPullRequest pullRequest);

    
    Task findTaskByKey(String taskKey, Long projectId);

    
    boolean shouldCloseTask(String text, String taskKey);

    
    String extractProjectKey(String taskKey);

    
    boolean validateTaskBelongsToProject(String taskKey, Long projectId);
}
