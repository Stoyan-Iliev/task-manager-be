package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.git.domain.GitCommit;
import com.gradproject.taskmanager.modules.git.domain.GitCommitTask;
import com.gradproject.taskmanager.modules.git.domain.enums.LinkMethod;
import com.gradproject.taskmanager.modules.git.dto.response.CommitResponse;
import com.gradproject.taskmanager.modules.git.repository.GitCommitRepository;
import com.gradproject.taskmanager.modules.git.repository.GitCommitTaskRepository;
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
public class GitCommitServiceImpl implements GitCommitService {

    private final GitCommitRepository gitCommitRepository;
    private final GitCommitTaskRepository gitCommitTaskRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final GitIntegrationMapper gitIntegrationMapper;
    private final PermissionService permissionService;

    @Override
    @Transactional(readOnly = true)
    public List<CommitResponse> getCommitsByTask(Long taskId, Integer userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canAccessProject(user, task.getProject())) {
            throw new UnauthorizedException("You don't have permission to view this task's commits");
        }

        List<GitCommit> commits = gitCommitRepository.findByTaskId(taskId);
        return commits.stream()
            .map(this::toCommitResponseWithLinkedTasks)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommitResponse> getCommitsByTask(Long taskId, Integer userId, Pageable pageable) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canAccessProject(user, task.getProject())) {
            throw new UnauthorizedException("You don't have permission to view this task's commits");
        }

        Page<GitCommit> commits = gitCommitRepository.findByTaskId(taskId, pageable);
        return commits.map(this::toCommitResponseWithLinkedTasks);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommitResponse> getCommitsByProject(Long projectId, Integer userId, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to view this project's commits");
        }

        
        
        Page<GitCommit> commits = gitCommitRepository.findAll(pageable);
        return commits.map(this::toCommitResponseWithLinkedTasks);
    }

    @Override
    @Transactional(readOnly = true)
    public CommitResponse getCommit(Long commitId, Integer userId) {
        GitCommit commit = gitCommitRepository.findById(commitId)
            .orElseThrow(() -> new ResourceNotFoundException("Commit not found with id: " + commitId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        Project project = commit.getGitIntegration().getProject();
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to view this commit");
        }

        return toCommitResponseWithLinkedTasks(commit);
    }

    @Override
    @Transactional(readOnly = true)
    public CommitResponse getCommitBySha(Long integrationId, String commitSha, Integer userId) {
        GitCommit commit = gitCommitRepository.findByGitIntegrationIdAndCommitSha(integrationId, commitSha)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Commit not found with SHA: " + commitSha + " for integration: " + integrationId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        Project project = commit.getGitIntegration().getProject();
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to view this commit");
        }

        return toCommitResponseWithLinkedTasks(commit);
    }

    @Override
    @Transactional
    public CommitResponse linkCommitToTask(Long commitId, Long taskId, Integer userId) {
        GitCommit commit = gitCommitRepository.findById(commitId)
            .orElseThrow(() -> new ResourceNotFoundException("Commit not found with id: " + commitId));

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = commit.getGitIntegration().getProject();

        
        if (!permissionService.canManageTasks(user, project)) {
            throw new UnauthorizedException("You don't have permission to link commits to tasks in this project");
        }

        
        if (!task.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Task must belong to the same project as the commit's integration");
        }

        
        if (gitCommitTaskRepository.existsByGitCommitIdAndTaskId(commitId, taskId)) {
            throw new IllegalArgumentException("Commit is already linked to this task");
        }

        
        GitCommitTask link = new GitCommitTask(commit, task, LinkMethod.MANUAL);
        gitCommitTaskRepository.save(link);

        return toCommitResponseWithLinkedTasks(commit);
    }

    @Override
    @Transactional
    public CommitResponse unlinkCommitFromTask(Long commitId, Long taskId, Integer userId) {
        GitCommit commit = gitCommitRepository.findById(commitId)
            .orElseThrow(() -> new ResourceNotFoundException("Commit not found with id: " + commitId));

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = commit.getGitIntegration().getProject();

        
        if (!permissionService.canManageTasks(user, project)) {
            throw new UnauthorizedException("You don't have permission to unlink commits from tasks in this project");
        }

        
        GitCommitTask link = gitCommitTaskRepository.findByGitCommitIdAndTaskId(commitId, taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Link not found between commit and task"));

        gitCommitTaskRepository.delete(link);

        return toCommitResponseWithLinkedTasks(commit);
    }

    
    private CommitResponse toCommitResponseWithLinkedTasks(GitCommit commit) {
        CommitResponse baseResponse = gitIntegrationMapper.toCommitResponse(commit);

        
        List<String> linkedTaskKeys = gitCommitTaskRepository.findByGitCommitId(commit.getId())
            .stream()
            .map(link -> link.getTask().getKey())
            .toList();

        
        return new CommitResponse(
            baseResponse.id(),
            baseResponse.gitIntegrationId(),
            baseResponse.commitSha(),
            baseResponse.shortSha(),
            baseResponse.parentSha(),
            baseResponse.branchName(),
            baseResponse.authorName(),
            baseResponse.authorEmail(),
            baseResponse.authorDate(),
            baseResponse.committerName(),
            baseResponse.committerEmail(),
            baseResponse.committerDate(),
            baseResponse.message(),
            baseResponse.messageBody(),
            baseResponse.linesAdded(),
            baseResponse.linesDeleted(),
            baseResponse.filesChanged(),
            baseResponse.commitUrl(),
            linkedTaskKeys,  
            baseResponse.smartCommands(),
            baseResponse.createdAt()
        );
    }
}
