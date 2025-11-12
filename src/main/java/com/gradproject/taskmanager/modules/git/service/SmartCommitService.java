package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.GitCommit;
import com.gradproject.taskmanager.modules.task.domain.Task;

import java.util.List;


public interface SmartCommitService {

    
    SmartCommitExecutionSummary processCommit(GitCommit commit);

    
    List<CommandExecutionResult> executeCommands(GitCommit commit, Task task);

    
    CommandExecutionResult executeCommand(
        String commandType,
        String commandValue,
        Task task,
        Integer executedBy
    );

    
    record SmartCommitExecutionSummary(
        int totalCommands,
        int successfulExecutions,
        int failedExecutions,
        List<CommandExecutionResult> results
    ) {}

    
    record CommandExecutionResult(
        String commandType,
        String commandText,
        boolean success,
        String errorMessage,
        Long executionId
    ) {}
}
