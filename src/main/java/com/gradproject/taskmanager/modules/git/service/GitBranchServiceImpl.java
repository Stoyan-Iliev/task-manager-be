package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.git.domain.GitBranch;
import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.domain.enums.BranchStatus;
import com.gradproject.taskmanager.modules.git.dto.request.CreateBranchRequest;
import com.gradproject.taskmanager.modules.git.dto.response.BranchResponse;
import com.gradproject.taskmanager.modules.git.repository.GitBranchRepository;
import com.gradproject.taskmanager.modules.git.repository.GitIntegrationRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.BranchMapper;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitBranchServiceImpl implements GitBranchService {

    private final GitBranchRepository gitBranchRepository;
    private final GitIntegrationRepository gitIntegrationRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final BranchMapper branchMapper;
    private final PermissionService permissionService;

    @Override
    @Transactional
    public BranchResponse createBranch(Long taskId, CreateBranchRequest request, Integer userId) {
        // Find task
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        // Find user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check permissions
        if (!permissionService.canEditProject(user, task.getProject())) {
            throw new UnauthorizedException("You don't have permission to create branches for this task");
        }

        // Find Git integration
        GitIntegration integration = gitIntegrationRepository.findById(request.gitIntegrationId())
            .orElseThrow(() -> new ResourceNotFoundException("Git integration not found with id: " + request.gitIntegrationId()));

        // Verify integration belongs to the same project
        if (!integration.getProject().getId().equals(task.getProject().getId())) {
            throw new IllegalArgumentException("Git integration must belong to the same project as the task");
        }

        // Generate branch name if not provided
        String branchName = request.branchName();
        if (branchName == null || branchName.isEmpty()) {
            // Use branch prefix from integration if configured
            String prefix = integration.getBranchPrefix();
            if (prefix == null || prefix.isEmpty()) {
                prefix = request.branchType() + "/";
            }
            branchName = prefix + task.getKey().toLowerCase();
        }

        // Check if branch already exists
        if (gitBranchRepository.existsByGitIntegrationIdAndBranchName(integration.getId(), branchName)) {
            throw new IllegalArgumentException("Branch already exists: " + branchName);
        }

        // Create branch entity
        GitBranch branch = new GitBranch(integration, task, branchName);
        branch.setBaseBranch(request.baseBranch());
        branch.setCreatedFromUi(true);
        branch.setCreatedBy(user);
        branch.setStatus(BranchStatus.ACTIVE);

        // TODO: Create branch on Git provider (GitHub/GitLab) via API
        // For now, branches are tracked locally only
        // Users should create the branch manually in their Git client or via the Git provider UI
        log.info("Branch {} will be tracked locally. Create it in your Git client with: git checkout -b {}",
                branchName, branchName);

        // Save to database
        branch = gitBranchRepository.save(branch);
        log.info("Created branch: {} for task: {}", branchName, task.getKey());

        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getBranchesByTask(Long taskId, Integer userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check permissions
        if (!permissionService.canAccessProject(user, task.getProject())) {
            throw new UnauthorizedException("You don't have permission to view this task's branches");
        }

        List<GitBranch> branches = gitBranchRepository.findByTaskId(taskId);
        return branches.stream()
            .map(branchMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getBranchesByProject(Long projectId, Integer userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check permissions
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to view this project's branches");
        }

        List<GitBranch> branches = gitBranchRepository.findByProjectAndStatus(projectId, BranchStatus.ACTIVE);
        return branches.stream()
            .map(branchMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getBranchesByIntegration(Long integrationId, Integer userId) {
        GitIntegration integration = gitIntegrationRepository.findById(integrationId)
            .orElseThrow(() -> new ResourceNotFoundException("Git integration not found with id: " + integrationId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check permissions
        if (!permissionService.canAccessProject(user, integration.getProject())) {
            throw new UnauthorizedException("You don't have permission to view this integration's branches");
        }

        List<GitBranch> branches = gitBranchRepository.findByGitIntegrationId(integrationId);
        return branches.stream()
            .map(branchMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranch(Long branchId, Integer userId) {
        GitBranch branch = gitBranchRepository.findById(branchId)
            .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check permissions
        if (!permissionService.canAccessProject(user, branch.getTask().getProject())) {
            throw new UnauthorizedException("You don't have permission to view this branch");
        }

        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional
    public void deleteBranch(Long branchId, Integer userId) {
        GitBranch branch = gitBranchRepository.findById(branchId)
            .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check permissions
        if (!permissionService.canEditProject(user, branch.getTask().getProject())) {
            throw new UnauthorizedException("You don't have permission to delete this branch");
        }

        // Mark as deleted instead of actually deleting
        branch.setStatus(BranchStatus.DELETED);
        branch.setDeletedAt(LocalDateTime.now());
        gitBranchRepository.save(branch);

        log.info("Marked branch {} as deleted", branch.getBranchName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getActiveBranchesByTask(Long taskId, Integer userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check permissions
        if (!permissionService.canAccessProject(user, task.getProject())) {
            throw new UnauthorizedException("You don't have permission to view this task's branches");
        }

        List<GitBranch> branches = gitBranchRepository.findActiveBranchesByTask(taskId);
        return branches.stream()
            .map(branchMapper::toResponse)
            .toList();
    }
}
