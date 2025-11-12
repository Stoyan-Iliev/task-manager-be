package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.GitCommit;
import com.gradproject.taskmanager.modules.git.domain.GitCommitTask;
import com.gradproject.taskmanager.modules.git.domain.SmartCommitExecution;
import com.gradproject.taskmanager.modules.git.domain.enums.SmartCommitCommandType;
import com.gradproject.taskmanager.modules.git.parser.SmartCommitParser;
import com.gradproject.taskmanager.modules.git.repository.GitCommitTaskRepository;
import com.gradproject.taskmanager.modules.git.repository.SmartCommitExecutionRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class SmartCommitServiceImpl implements SmartCommitService {

    private final SmartCommitParser smartCommitParser;
    private final GitCommitTaskRepository commitTaskRepository;
    private final SmartCommitExecutionRepository executionRepository;

    @Override
    @Transactional
    public SmartCommitExecutionSummary processCommit(GitCommit commit) {
        log.info("Processing smart commit commands for commit {}", commit.getCommitSha());

        
        List<SmartCommitParser.SmartCommitCommand> commands =
            smartCommitParser.parseCommands(commit.getMessage());

        if (commands.isEmpty()) {
            log.debug("No smart commit commands found in commit {}", commit.getCommitSha());
            return new SmartCommitExecutionSummary(0, 0, 0, List.of());
        }

        log.info("Found {} smart commit commands in commit {}", commands.size(), commit.getCommitSha());

        
        List<GitCommitTask> linkedTasks = commitTaskRepository.findByGitCommitId(commit.getId());

        if (linkedTasks.isEmpty()) {
            log.warn("Commit {} has smart commands but no linked tasks", commit.getCommitSha());
            return new SmartCommitExecutionSummary(commands.size(), 0, commands.size(), List.of());
        }

        
        List<CommandExecutionResult> allResults = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (GitCommitTask linkedTask : linkedTasks) {
            List<CommandExecutionResult> taskResults = executeCommands(commit, linkedTask.getTask());
            allResults.addAll(taskResults);

            for (CommandExecutionResult result : taskResults) {
                if (result.success()) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
        }

        log.info("Executed {} commands: {} successful, {} failed",
            commands.size(), successCount, failCount);

        return new SmartCommitExecutionSummary(
            commands.size(),
            successCount,
            failCount,
            allResults
        );
    }

    @Override
    @Transactional
    public List<CommandExecutionResult> executeCommands(GitCommit commit, Task task) {
        log.info("Executing smart commands for task {} from commit {}",
            task.getKey(), commit.getCommitSha());

        
        List<SmartCommitParser.SmartCommitCommand> commands =
            smartCommitParser.parseCommands(commit.getMessage());

        List<CommandExecutionResult> results = new ArrayList<>();

        for (SmartCommitParser.SmartCommitCommand command : commands) {
            
            
            Integer executedBy = null;

            CommandExecutionResult result = executeCommand(
                command.getType().name(),
                command.getValue(),
                task,
                executedBy
            );

            
            SmartCommitExecution execution = new SmartCommitExecution(
                commit,
                task,
                command.getType(),
                command.getOriginalText()
            );

            if (result.success()) {
                execution.setExecuted(true);
                execution.setExecutedAt(LocalDateTime.now());
                
            } else {
                execution.setExecuted(false);
                execution.setExecutionError(result.errorMessage());
            }

            executionRepository.save(execution);
            results.add(new CommandExecutionResult(
                command.getType().name(),
                command.getOriginalText(),
                result.success(),
                result.errorMessage(),
                execution.getId()
            ));
        }

        return results;
    }

    @Override
    @Transactional
    public CommandExecutionResult executeCommand(
        String commandType,
        String commandValue,
        Task task,
        Integer executedBy
    ) {
        SmartCommitCommandType type;
        try {
            type = SmartCommitCommandType.valueOf(commandType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return new CommandExecutionResult(
                commandType,
                commandValue,
                false,
                "Unknown command type: " + commandType,
                null
            );
        }

        log.debug("Executing {} command on task {}: {}", type, task.getKey(), commandValue);

        try {
            switch (type) {
                case TRANSITION -> {
                    return executeTransition(task, commandValue, executedBy);
                }
                case COMMENT -> {
                    return executeComment(task, commandValue, executedBy);
                }
                case TIME -> {
                    return executeTimeLog(task, commandValue, executedBy);
                }
                case ASSIGN -> {
                    return executeAssign(task, commandValue, executedBy);
                }
                case LABEL -> {
                    return executeLabel(task, commandValue, executedBy);
                }
                default -> {
                    return new CommandExecutionResult(
                        type.name(),
                        commandValue,
                        false,
                        "Command type not implemented: " + type,
                        null
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error executing command {} on task {}", type, task.getKey(), e);
            return new CommandExecutionResult(
                type.name(),
                commandValue,
                false,
                "Execution error: " + e.getMessage(),
                null
            );
        }
    }

    
    private CommandExecutionResult executeTransition(Task task, String statusName, Integer executedBy) {
        
        
        
        
        

        log.warn("Transition command not yet implemented: {} for task {}", statusName, task.getKey());
        return new CommandExecutionResult(
            "TRANSITION",
            statusName,
            false,
            "Transition command not yet implemented (requires TaskService integration)",
            null
        );
    }

    
    private CommandExecutionResult executeComment(Task task, String commentText, Integer executedBy) {
        
        
        
        

        log.warn("Comment command not yet implemented for task {}", task.getKey());
        return new CommandExecutionResult(
            "COMMENT",
            commentText,
            false,
            "Comment command not yet implemented (requires CommentService integration)",
            null
        );
    }

    
    private CommandExecutionResult executeTimeLog(Task task, String timeValue, Integer executedBy) {
        
        
        
        
        

        log.warn("Time log command not yet implemented for task {}", task.getKey());
        return new CommandExecutionResult(
            "TIME",
            timeValue,
            false,
            "Time log command not yet implemented (requires WorkLogService integration)",
            null
        );
    }

    
    private CommandExecutionResult executeAssign(Task task, String username, Integer executedBy) {
        
        
        
        
        
        

        log.warn("Assign command not yet implemented for task {}", task.getKey());
        return new CommandExecutionResult(
            "ASSIGN",
            username,
            false,
            "Assign command not yet implemented (requires TaskService integration)",
            null
        );
    }

    
    private CommandExecutionResult executeLabel(Task task, String labelName, Integer executedBy) {
        
        
        
        

        log.warn("Label command not yet implemented for task {}", task.getKey());
        return new CommandExecutionResult(
            "LABEL",
            labelName,
            false,
            "Label command not yet implemented (requires LabelService integration)",
            null
        );
    }
}
