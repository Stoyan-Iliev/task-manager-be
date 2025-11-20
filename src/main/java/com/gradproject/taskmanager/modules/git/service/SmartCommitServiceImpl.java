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
    private final com.gradproject.taskmanager.modules.task.service.TaskService taskService;
    private final com.gradproject.taskmanager.modules.task.service.CommentService commentService;
    private final com.gradproject.taskmanager.modules.task.service.LabelService labelService;
    private final com.gradproject.taskmanager.modules.project.repository.TaskStatusRepository taskStatusRepository;
    private final com.gradproject.taskmanager.modules.task.repository.LabelRepository labelRepository;
    private final com.gradproject.taskmanager.modules.auth.repository.UserRepository userRepository;

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
        try {
            // Find matching status in the project
            List<com.gradproject.taskmanager.modules.project.domain.TaskStatus> statuses =
                    taskStatusRepository.findByProjectIdOrderByOrderIndexAsc(task.getProject().getId());

            com.gradproject.taskmanager.modules.project.domain.TaskStatus targetStatus = statuses.stream()
                    .filter(s -> s.getName().equalsIgnoreCase(statusName))
                    .findFirst()
                    .orElse(null);

            if (targetStatus == null) {
                return new CommandExecutionResult(
                        "TRANSITION",
                        statusName,
                        false,
                        "Status not found: " + statusName,
                        null
                );
            }

            // Use a system user ID (1) if executedBy is null
            // In production, you might want to create a dedicated "system" user for git operations
            Integer userId = executedBy != null ? executedBy : 1;

            // Create transition request
            com.gradproject.taskmanager.modules.task.dto.TaskTransitionRequest request =
                    new com.gradproject.taskmanager.modules.task.dto.TaskTransitionRequest(
                            targetStatus.getId(),
                            "Transitioned via smart commit"
                    );

            // Execute transition
            taskService.transitionStatus(task.getId(), request, userId);

            log.info("Transitioned task {} to status: {}", task.getKey(), statusName);
            return new CommandExecutionResult(
                    "TRANSITION",
                    statusName,
                    true,
                    null,
                    null
            );

        } catch (Exception e) {
            log.error("Error transitioning task {} to {}", task.getKey(), statusName, e);
            return new CommandExecutionResult(
                    "TRANSITION",
                    statusName,
                    false,
                    e.getMessage(),
                    null
            );
        }
    }

    private CommandExecutionResult executeComment(Task task, String commentText, Integer executedBy) {
        try {
            // Use a system user ID (1) if executedBy is null
            Integer userId = executedBy != null ? executedBy : 1;

            // Create comment request
            com.gradproject.taskmanager.modules.task.dto.CommentRequest request =
                    new com.gradproject.taskmanager.modules.task.dto.CommentRequest(
                            commentText,
                            null  // Not a reply
                    );

            // Add comment
            commentService.addComment(task.getId(), request, userId);

            log.info("Added comment to task {} via smart commit", task.getKey());
            return new CommandExecutionResult(
                    "COMMENT",
                    commentText,
                    true,
                    null,
                    null
            );

        } catch (Exception e) {
            log.error("Error adding comment to task {}", task.getKey(), e);
            return new CommandExecutionResult(
                    "COMMENT",
                    commentText,
                    false,
                    e.getMessage(),
                    null
            );
        }
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
        try {
            // Remove @ symbol if present
            String cleanUsername = username.startsWith("@") ? username.substring(1) : username;

            // Find user by username
            com.gradproject.taskmanager.modules.auth.domain.User assignee =
                    userRepository.findByUsername(cleanUsername).orElse(null);

            if (assignee == null) {
                return new CommandExecutionResult(
                        "ASSIGN",
                        username,
                        false,
                        "User not found: " + cleanUsername,
                        null
                );
            }

            // Use a system user ID (1) if executedBy is null
            Integer userId = executedBy != null ? executedBy : 1;

            // Create assign request
            com.gradproject.taskmanager.modules.task.dto.TaskAssignRequest request =
                    new com.gradproject.taskmanager.modules.task.dto.TaskAssignRequest(assignee.getId());

            // Execute assignment
            taskService.assignTask(task.getId(), request, userId);

            log.info("Assigned task {} to user: {}", task.getKey(), cleanUsername);
            return new CommandExecutionResult(
                    "ASSIGN",
                    username,
                    true,
                    null,
                    null
            );

        } catch (Exception e) {
            log.error("Error assigning task {} to {}", task.getKey(), username, e);
            return new CommandExecutionResult(
                    "ASSIGN",
                    username,
                    false,
                    e.getMessage(),
                    null
            );
        }
    }

    private CommandExecutionResult executeLabel(Task task, String labelName, Integer executedBy) {
        try {
            Long orgId = task.getProject().getOrganization().getId();

            // Find or create label (case-insensitive search)
            com.gradproject.taskmanager.modules.task.domain.Label label =
                    labelRepository.findByOrganizationIdAndName(orgId, labelName)
                            .orElse(null);

            if (label == null) {
                // Create label if it doesn't exist
                Integer userId = executedBy != null ? executedBy : 1;

                com.gradproject.taskmanager.modules.task.dto.LabelRequest request =
                        new com.gradproject.taskmanager.modules.task.dto.LabelRequest(
                                labelName,
                                "#808080",  // Default gray color
                                null  // No description
                        );

                com.gradproject.taskmanager.modules.task.dto.LabelResponse response =
                        labelService.createLabel(orgId, request, userId);

                label = labelRepository.findById(response.id()).orElse(null);

                if (label == null) {
                    return new CommandExecutionResult(
                            "LABEL",
                            labelName,
                            false,
                            "Failed to create label",
                            null
                    );
                }

                log.info("Created new label: {}", labelName);
            }

            // Add label to task
            Integer userId = executedBy != null ? executedBy : 1;
            labelService.addLabelToTask(task.getId(), label.getId(), userId);

            log.info("Added label {} to task {}", labelName, task.getKey());
            return new CommandExecutionResult(
                    "LABEL",
                    labelName,
                    true,
                    null,
                    null
            );

        } catch (Exception e) {
            log.error("Error adding label {} to task {}", labelName, task.getKey(), e);
            return new CommandExecutionResult(
                    "LABEL",
                    labelName,
                    false,
                    e.getMessage(),
                    null
            );
        }
    }
}
