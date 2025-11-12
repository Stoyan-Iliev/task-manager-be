package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.git.domain.GitPrTask;
import com.gradproject.taskmanager.modules.git.domain.GitPullRequest;
import com.gradproject.taskmanager.modules.git.domain.enums.LinkMethod;
import com.gradproject.taskmanager.modules.git.dto.response.PullRequestResponse;
import com.gradproject.taskmanager.modules.git.repository.GitPrTaskRepository;
import com.gradproject.taskmanager.modules.git.repository.GitPullRequestRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.GitIntegrationMapper;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class GitPullRequestServiceImpl implements GitPullRequestService {

    private final GitPullRequestRepository gitPullRequestRepository;
    private final GitPrTaskRepository gitPrTaskRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final GitIntegrationMapper gitIntegrationMapper;
    private final PermissionService permissionService;

    @Override
    @Transactional(readOnly = true)
    public List<PullRequestResponse> getPullRequestsByTask(Long taskId, Integer userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canAccessProject(user, task.getProject())) {
            throw new UnauthorizedException("You don't have permission to view this task's pull requests");
        }

        List<GitPullRequest> pullRequests = gitPullRequestRepository.findByTaskId(taskId);
        return pullRequests.stream()
            .map(this::toPullRequestResponseWithLinkedTasks)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PullRequestResponse> getPullRequestsByProject(Long projectId, Integer userId, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to view this project's pull requests");
        }

        
        
        Page<GitPullRequest> pullRequests = gitPullRequestRepository.findAll(pageable);
        return pullRequests.map(this::toPullRequestResponseWithLinkedTasks);
    }

    @Override
    @Transactional(readOnly = true)
    public PullRequestResponse getPullRequest(Long prId, Integer userId) {
        GitPullRequest pullRequest = gitPullRequestRepository.findById(prId)
            .orElseThrow(() -> new ResourceNotFoundException("Pull request not found with id: " + prId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        Project project = pullRequest.getGitIntegration().getProject();
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to view this pull request");
        }

        return toPullRequestResponseWithLinkedTasks(pullRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public PullRequestResponse getPullRequestByNumber(Long integrationId, Integer prNumber, Integer userId) {
        GitPullRequest pullRequest = gitPullRequestRepository.findByGitIntegrationIdAndPrNumber(integrationId, prNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Pull request not found with number: " + prNumber + " for integration: " + integrationId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        Project project = pullRequest.getGitIntegration().getProject();
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to view this pull request");
        }

        return toPullRequestResponseWithLinkedTasks(pullRequest);
    }

    @Override
    @Transactional
    public PullRequestResponse linkPullRequestToTask(Long prId, Long taskId, Boolean closesTask, Integer userId) {
        GitPullRequest pullRequest = gitPullRequestRepository.findById(prId)
            .orElseThrow(() -> new ResourceNotFoundException("Pull request not found with id: " + prId));

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = pullRequest.getGitIntegration().getProject();

        
        if (!permissionService.canManageTasks(user, project)) {
            throw new UnauthorizedException("You don't have permission to link pull requests to tasks in this project");
        }

        
        if (!task.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Task must belong to the same project as the pull request's integration");
        }

        
        if (gitPrTaskRepository.existsByGitPullRequestIdAndTaskId(prId, taskId)) {
            throw new IllegalArgumentException("Pull request is already linked to this task");
        }

        
        GitPrTask link = new GitPrTask(pullRequest, task, LinkMethod.MANUAL, closesTask != null ? closesTask : false);
        gitPrTaskRepository.save(link);

        return toPullRequestResponseWithLinkedTasks(pullRequest);
    }

    @Override
    @Transactional
    public PullRequestResponse unlinkPullRequestFromTask(Long prId, Long taskId, Integer userId) {
        GitPullRequest pullRequest = gitPullRequestRepository.findById(prId)
            .orElseThrow(() -> new ResourceNotFoundException("Pull request not found with id: " + prId));

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = pullRequest.getGitIntegration().getProject();

        
        if (!permissionService.canManageTasks(user, project)) {
            throw new UnauthorizedException("You don't have permission to unlink pull requests from tasks in this project");
        }

        
        GitPrTask link = gitPrTaskRepository.findByGitPullRequestIdAndTaskId(prId, taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Link not found between pull request and task"));

        gitPrTaskRepository.delete(link);

        return toPullRequestResponseWithLinkedTasks(pullRequest);
    }

    
    private PullRequestResponse toPullRequestResponseWithLinkedTasks(GitPullRequest pullRequest) {
        PullRequestResponse baseResponse = gitIntegrationMapper.toPullRequestResponse(pullRequest);

        
        List<GitPrTask> links = gitPrTaskRepository.findByGitPullRequestId(pullRequest.getId());
        List<String> linkedTaskKeys = links.stream()
            .map(link -> link.getTask().getKey())
            .toList();

        
        boolean closesTask = links.stream().anyMatch(GitPrTask::getClosesTask);

        
        return new PullRequestResponse(
            baseResponse.id(),
            baseResponse.gitIntegrationId(),
            baseResponse.gitBranchId(),
            baseResponse.prNumber(),
            baseResponse.prTitle(),
            baseResponse.prDescription(),
            baseResponse.prUrl(),
            baseResponse.status(),
            baseResponse.sourceBranch(),
            baseResponse.targetBranch(),
            baseResponse.headCommitSha(),
            baseResponse.authorUsername(),
            baseResponse.authorName(),
            baseResponse.authorEmail(),
            baseResponse.reviewers(),
            baseResponse.approvalsCount(),
            baseResponse.requiredApprovals(),
            baseResponse.approved(),
            baseResponse.checksStatus(),
            baseResponse.checksCount(),
            baseResponse.checksPassed(),
            baseResponse.checks(),
            baseResponse.allChecksPassed(),
            baseResponse.mergeable(),
            baseResponse.merged(),
            baseResponse.mergedAt(),
            baseResponse.mergedBy(),
            baseResponse.mergeCommitSha(),
            linkedTaskKeys,  
            closesTask,  
            baseResponse.createdAt(),
            baseResponse.updatedAt(),
            baseResponse.closedAt()
        );
    }
}
