package com.gradproject.taskmanager.modules.release.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.release.domain.Release;
import com.gradproject.taskmanager.modules.release.domain.ReleaseTask;
import com.gradproject.taskmanager.modules.release.domain.enums.ReleaseStatus;
import com.gradproject.taskmanager.modules.release.dto.CreateReleaseRequest;
import com.gradproject.taskmanager.modules.release.dto.ReleaseResponse;
import com.gradproject.taskmanager.modules.release.dto.UpdateReleaseRequest;
import com.gradproject.taskmanager.modules.release.repository.ReleaseRepository;
import com.gradproject.taskmanager.modules.release.repository.ReleaseTaskRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.DuplicateResourceException;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.ReleaseMapper;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseServiceImpl implements ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final ReleaseTaskRepository releaseTaskRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ReleaseMapper releaseMapper;

    @Override
    @Transactional
    public ReleaseResponse createRelease(Long projectId, CreateReleaseRequest request, Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        
        if (!permissionService.canEditProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to create releases for this project");
        }

        
        if (releaseRepository.existsByProjectIdAndName(projectId, request.name())) {
            throw new DuplicateResourceException("Release with name '" + request.name() + "' already exists in this project");
        }

        Release release = new Release(project, request.name(), request.version(), request.releaseDate(), user);
        release.setDescription(request.description());

        Release saved = releaseRepository.save(release);
        log.info("Created release: {} for project: {} by user: {}", saved.getId(), projectId, userId);

        return enrichReleaseResponse(releaseMapper.toResponse(saved), saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReleaseResponse> getProjectReleases(Long projectId, Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to view releases for this project");
        }

        List<Release> releases = releaseRepository.findByProjectId(projectId);

        return releases.stream()
            .map(release -> enrichReleaseResponse(releaseMapper.toResponse(release), release.getId()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReleaseResponse> getProjectReleasesByStatus(Long projectId, ReleaseStatus status, Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to view releases for this project");
        }

        List<Release> releases = releaseRepository.findByProjectIdAndStatus(projectId, status);

        return releases.stream()
            .map(release -> enrichReleaseResponse(releaseMapper.toResponse(release), release.getId()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReleaseResponse getRelease(Long releaseId, Integer userId) {
        Release release = releaseRepository.findById(releaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canAccessProject(user, release.getProject())) {
            throw new UnauthorizedException("You don't have permission to view this release");
        }

        return enrichReleaseResponse(releaseMapper.toResponse(release), releaseId);
    }

    @Override
    @Transactional
    public ReleaseResponse updateRelease(Long releaseId, UpdateReleaseRequest request, Integer userId) {
        Release release = releaseRepository.findById(releaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canEditProject(user, release.getProject())) {
            throw new UnauthorizedException("You don't have permission to update this release");
        }

        
        if (request.name() != null && !request.name().equals(release.getName())) {
            if (releaseRepository.existsByProjectIdAndNameExcludingId(release.getProject().getId(), request.name(), releaseId)) {
                throw new DuplicateResourceException("Release with name '" + request.name() + "' already exists in this project");
            }
            release.setName(request.name());
        }

        if (request.description() != null) {
            release.setDescription(request.description());
        }
        if (request.version() != null) {
            release.setVersion(request.version());
        }
        if (request.releaseDate() != null) {
            release.setReleaseDate(request.releaseDate());
        }
        if (request.status() != null) {
            release.setStatus(request.status());

            
            if (request.status() == ReleaseStatus.RELEASED && release.getReleasedAt() == null) {
                release.setReleasedAt(LocalDateTime.now());
            }

            
            if (request.status() == ReleaseStatus.ARCHIVED && release.getArchivedAt() == null) {
                release.setArchivedAt(LocalDateTime.now());
            }
        }

        Release updated = releaseRepository.save(release);
        log.info("Updated release: {} by user: {}", releaseId, userId);

        return enrichReleaseResponse(releaseMapper.toResponse(updated), releaseId);
    }

    @Override
    @Transactional
    public void deleteRelease(Long releaseId, Integer userId) {
        Release release = releaseRepository.findById(releaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canEditProject(user, release.getProject())) {
            throw new UnauthorizedException("You don't have permission to delete this release");
        }

        releaseRepository.delete(release);
        log.info("Deleted release: {} by user: {}", releaseId, userId);
    }

    @Override
    @Transactional
    public ReleaseResponse addTaskToRelease(Long releaseId, Long taskId, Integer userId) {
        Release release = releaseRepository.findById(releaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canManageTasks(user, release.getProject())) {
            throw new UnauthorizedException("You don't have permission to add tasks to this release");
        }

        
        if (!task.getProject().getId().equals(release.getProject().getId())) {
            throw new IllegalArgumentException("Task must belong to the same project as the release");
        }

        
        if (releaseTaskRepository.existsByReleaseIdAndTaskId(releaseId, taskId)) {
            throw new DuplicateResourceException("Task is already in this release");
        }

        ReleaseTask releaseTask = new ReleaseTask(release, task, user);
        releaseTaskRepository.save(releaseTask);

        log.info("Added task: {} to release: {} by user: {}", taskId, releaseId, userId);

        return enrichReleaseResponse(releaseMapper.toResponse(release), releaseId);
    }

    @Override
    @Transactional
    public ReleaseResponse removeTaskFromRelease(Long releaseId, Long taskId, Integer userId) {
        Release release = releaseRepository.findById(releaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canManageTasks(user, release.getProject())) {
            throw new UnauthorizedException("You don't have permission to remove tasks from this release");
        }

        ReleaseTask releaseTask = releaseTaskRepository.findByReleaseIdAndTaskId(releaseId, taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task is not in this release"));

        releaseTaskRepository.delete(releaseTask);
        log.info("Removed task: {} from release: {} by user: {}", taskId, releaseId, userId);

        return enrichReleaseResponse(releaseMapper.toResponse(release), releaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getReleaseTasks(Long releaseId, Integer userId) {
        Release release = releaseRepository.findById(releaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canAccessProject(user, release.getProject())) {
            throw new UnauthorizedException("You don't have permission to view this release's tasks");
        }

        List<ReleaseTask> releaseTasks = releaseTaskRepository.findByReleaseId(releaseId);

        return releaseTasks.stream()
            .map(rt -> rt.getTask().getId())
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReleaseResponse markReleaseAsReleased(Long releaseId, Integer userId) {
        Release release = releaseRepository.findById(releaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canEditProject(user, release.getProject())) {
            throw new UnauthorizedException("You don't have permission to mark this release as released");
        }

        release.setStatus(ReleaseStatus.RELEASED);
        release.setReleasedAt(LocalDateTime.now());

        Release updated = releaseRepository.save(release);
        log.info("Marked release: {} as RELEASED by user: {}", releaseId, userId);

        return enrichReleaseResponse(releaseMapper.toResponse(updated), releaseId);
    }

    @Override
    @Transactional
    public ReleaseResponse archiveRelease(Long releaseId, Integer userId) {
        Release release = releaseRepository.findById(releaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + releaseId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canEditProject(user, release.getProject())) {
            throw new UnauthorizedException("You don't have permission to archive this release");
        }

        release.setStatus(ReleaseStatus.ARCHIVED);
        release.setArchivedAt(LocalDateTime.now());

        Release updated = releaseRepository.save(release);
        log.info("Archived release: {} by user: {}", releaseId, userId);

        return enrichReleaseResponse(releaseMapper.toResponse(updated), releaseId);
    }

    
    private ReleaseResponse enrichReleaseResponse(ReleaseResponse response, Long releaseId) {
        long taskCount = releaseTaskRepository.countByReleaseId(releaseId);
        long completedTaskCount = releaseTaskRepository.countCompletedByReleaseId(releaseId);

        return new ReleaseResponse(
            response.id(),
            response.projectId(),
            response.projectName(),
            response.name(),
            response.description(),
            response.version(),
            response.releaseDate(),
            response.status(),
            response.createdById(),
            response.createdByUsername(),
            response.createdAt(),
            response.updatedAt(),
            response.releasedAt(),
            response.archivedAt(),
            taskCount,
            completedTaskCount
        );
    }
}
