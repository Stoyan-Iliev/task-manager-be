package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.*;
import com.gradproject.taskmanager.modules.git.domain.enums.LinkMethod;
import com.gradproject.taskmanager.modules.git.parser.BranchNameParser;
import com.gradproject.taskmanager.modules.git.parser.IssueReferenceParser;
import com.gradproject.taskmanager.modules.git.repository.GitBranchRepository;
import com.gradproject.taskmanager.modules.git.repository.GitCommitTaskRepository;
import com.gradproject.taskmanager.modules.git.repository.GitPrTaskRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
@RequiredArgsConstructor
public class GitLinkingServiceImpl implements GitLinkingService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final GitBranchRepository branchRepository;
    private final GitCommitTaskRepository commitTaskRepository;
    private final GitPrTaskRepository prTaskRepository;
    private final IssueReferenceParser issueReferenceParser;
    private final BranchNameParser branchNameParser;

    
    private static final Pattern CLOSES_PATTERN = Pattern.compile(
        "(?i)\\b(close[sd]?|fix(es|ed)?|resolve[sd]?)\\s+([A-Z]{2,11}-\\d+)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    @Transactional
    public Task linkBranchToTask(GitBranch branch) {
        log.info("Linking branch {} to task", branch.getBranchName());

        
        String taskKey = branchNameParser.extractTaskReference(branch.getBranchName());

        if (taskKey == null) {
            log.debug("No task reference found in branch name: {}", branch.getBranchName());
            return null;
        }

        
        Long organizationId = branch.getGitIntegration().getOrganization().getId();

        
        Task task = taskRepository.findByOrganizationIdAndKey(organizationId, taskKey)
            .orElse(null);

        if (task == null) {
            log.warn("Task not found for key {} in organization {}", taskKey, organizationId);
            return null;
        }

        
        branch.setTask(task);
        branchRepository.save(branch);

        log.info("Successfully linked branch {} to task {}", branch.getBranchName(), taskKey);
        return task;
    }

    @Override
    @Transactional
    public List<Task> linkCommitToTasks(GitCommit commit) {
        log.info("Linking commit {} to tasks", commit.getCommitSha());

        List<Task> linkedTasks = new ArrayList<>();

        
        List<String> taskKeys = issueReferenceParser.extractReferences(commit.getMessage());

        if (taskKeys.isEmpty()) {
            log.debug("No task references found in commit message");
            return linkedTasks;
        }

        
        Long organizationId = commit.getGitIntegration().getOrganization().getId();

        
        for (String taskKey : taskKeys) {
            
            Task task = taskRepository.findByOrganizationIdAndKey(organizationId, taskKey)
                .orElse(null);

            if (task == null) {
                log.warn("Task not found for key {} in organization {}", taskKey, organizationId);
                continue;
            }

            
            boolean linkExists = commitTaskRepository.existsByGitCommitIdAndTaskId(
                commit.getId(), task.getId()
            );

            if (linkExists) {
                log.debug("Link already exists between commit {} and task {}",
                    commit.getCommitSha(), taskKey);
                linkedTasks.add(task);
                continue;
            }

            
            GitCommitTask link = new GitCommitTask(commit, task, LinkMethod.COMMIT_MESSAGE);
            commitTaskRepository.save(link);

            linkedTasks.add(task);
            log.info("Successfully linked commit {} to task {}", commit.getCommitSha(), taskKey);
        }

        return linkedTasks;
    }

    @Override
    @Transactional
    public List<Task> linkPullRequestToTasks(GitPullRequest pullRequest) {
        log.info("Linking pull request #{} to tasks", pullRequest.getPrNumber());

        List<Task> linkedTasks = new ArrayList<>();

        
        List<String> taskKeysFromTitle = issueReferenceParser.extractReferences(pullRequest.getPrTitle());

        
        List<String> taskKeysFromDescription = pullRequest.getPrDescription() != null ?
            issueReferenceParser.extractReferences(pullRequest.getPrDescription()) : new ArrayList<>();

        
        List<String> allTaskKeys = new ArrayList<>(taskKeysFromTitle);
        for (String key : taskKeysFromDescription) {
            if (!allTaskKeys.contains(key)) {
                allTaskKeys.add(key);
            }
        }

        if (allTaskKeys.isEmpty()) {
            log.debug("No task references found in PR title or description");
            return linkedTasks;
        }

        
        Long organizationId = pullRequest.getGitIntegration().getOrganization().getId();

        
        for (String taskKey : allTaskKeys) {
            Task task = taskRepository.findByOrganizationIdAndKey(organizationId, taskKey)
                .orElse(null);

            if (task == null) {
                log.warn("Task not found for key {} in organization {}", taskKey, organizationId);
                continue;
            }

            
            boolean linkExists = prTaskRepository.existsByGitPullRequestIdAndTaskId(
                pullRequest.getId(), task.getId()
            );

            if (linkExists) {
                log.debug("Link already exists between PR {} and task {}",
                    pullRequest.getPrNumber(), taskKey);
                linkedTasks.add(task);
                continue;
            }

            
            boolean shouldClose = shouldCloseTask(pullRequest.getPrTitle(), taskKey) ||
                (pullRequest.getPrDescription() != null &&
                 shouldCloseTask(pullRequest.getPrDescription(), taskKey));

            
            LinkMethod linkMethod = taskKeysFromTitle.contains(taskKey) ?
                LinkMethod.PR_TITLE : LinkMethod.PR_DESCRIPTION;

            
            GitPrTask link = new GitPrTask(pullRequest, task, linkMethod, shouldClose);
            prTaskRepository.save(link);

            linkedTasks.add(task);
            log.info("Successfully linked PR #{} to task {} (closes: {})",
                pullRequest.getPrNumber(), taskKey, shouldClose);
        }

        return linkedTasks;
    }

    @Override
    public Task findTaskByKey(String taskKey, Long projectId) {
        
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            log.warn("Project not found: {}", projectId);
            return null;
        }

        Long organizationId = project.getOrganization().getId();

        
        String projectKey = extractProjectKey(taskKey);
        if (!project.getKey().equals(projectKey)) {
            log.warn("Task key {} does not match project key {}", taskKey, project.getKey());
            return null;
        }

        return taskRepository.findByOrganizationIdAndKey(organizationId, taskKey)
            .orElse(null);
    }

    @Override
    public boolean shouldCloseTask(String text, String taskKey) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        Matcher matcher = CLOSES_PATTERN.matcher(text);
        while (matcher.find()) {
            String matchedTaskKey = matcher.group(3);
            if (matchedTaskKey.equalsIgnoreCase(taskKey)) {
                log.debug("Found closing keyword for task {}: {}", taskKey, matcher.group(1));
                return true;
            }
        }

        return false;
    }

    @Override
    public String extractProjectKey(String taskKey) {
        if (taskKey == null || !taskKey.contains("-")) {
            return null;
        }

        int dashIndex = taskKey.indexOf('-');
        return taskKey.substring(0, dashIndex);
    }

    @Override
    public boolean validateTaskBelongsToProject(String taskKey, Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return false;
        }

        String projectKey = extractProjectKey(taskKey);
        return project.getKey().equals(projectKey);
    }
}
