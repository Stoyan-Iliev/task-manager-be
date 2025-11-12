package com.gradproject.taskmanager.modules.project.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.Sprint;
import com.gradproject.taskmanager.modules.project.domain.SprintStatus;
import com.gradproject.taskmanager.modules.project.dto.CompleteSprintRequest;
import com.gradproject.taskmanager.modules.project.dto.SprintMetrics;
import com.gradproject.taskmanager.modules.project.dto.SprintRequest;
import com.gradproject.taskmanager.modules.project.dto.SprintResponse;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.project.repository.SprintRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.ProjectMapper;
import com.gradproject.taskmanager.shared.mapper.TaskMapper;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PermissionService permissionService;
    private final ProjectMapper mapper;
    private final TaskMapper taskMapper;

    @Override
    @Transactional
    public SprintResponse createSprint(Long projectId, SprintRequest request, Integer userId) {
        log.info("Creating sprint '{}' in project {}", request.name(), projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canEditProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to manage sprints");
        }

        
        if (!request.isValidDateRange()) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        
        Sprint sprint = new Sprint(project, request.name(), request.goal(),
                request.startDate(), request.endDate(), user);
        sprint = sprintRepository.save(sprint);

        log.info("Successfully created sprint '{}'", request.name());
        return mapper.toSprintResponse(sprint);
    }

    @Override
    @Transactional
    public SprintResponse updateSprint(Long sprintId, SprintRequest request, Integer userId) {
        log.info("Updating sprint {}", sprintId);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canEditProject(user, sprint.getProject())) {
            throw new UnauthorizedException("You don't have permission to manage sprints");
        }

        
        if (!request.isValidDateRange()) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        
        sprint.setName(request.name());
        sprint.setGoal(request.goal());
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());
        sprint = sprintRepository.save(sprint);

        log.info("Successfully updated sprint {}", sprintId);
        return mapper.toSprintResponse(sprint);
    }

    @Override
    @Transactional
    public void deleteSprint(Long sprintId, Integer userId) {
        log.info("Deleting sprint {}", sprintId);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canEditProject(user, sprint.getProject())) {
            throw new UnauthorizedException("You don't have permission to manage sprints");
        }

        sprintRepository.delete(sprint);
        log.info("Successfully deleted sprint {}", sprintId);
    }

    @Override
    @Transactional(readOnly = true)
    public SprintResponse getSprint(Long sprintId, Integer requesterId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", requesterId));

        
        if (!permissionService.canAccessProject(requester, sprint.getProject())) {
            throw new UnauthorizedException("You don't have access to this sprint");
        }

        SprintResponse response = mapper.toSprintResponse(sprint);
        return attachMetrics(response, sprintId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SprintResponse> listProjectSprints(Long projectId, Integer requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", requesterId));

        
        if (!permissionService.canAccessProject(requester, project)) {
            throw new UnauthorizedException("You don't have access to this project");
        }

        List<Sprint> sprints = sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);
        return sprints.stream()
                .map(sprint -> {
                    SprintResponse response = mapper.toSprintResponse(sprint);
                    return attachMetrics(response, sprint.getId());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SprintResponse startSprint(Long sprintId, Integer userId) {
        log.info("Starting sprint {}", sprintId);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canEditProject(user, sprint.getProject())) {
            throw new UnauthorizedException("You don't have permission to manage sprints");
        }

        
        if (sprint.getStatus() != SprintStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED sprints can be started. Current status: " + sprint.getStatus());
        }

        
        long activeSprintCount = sprintRepository.countActiveSprintsByProject(sprint.getProject().getId());
        if (activeSprintCount > 0) {
            throw new IllegalStateException("Cannot start sprint. Another sprint is already active in this project.");
        }

        
        sprint.setStatus(SprintStatus.ACTIVE);
        sprint = sprintRepository.save(sprint);

        log.info("Successfully started sprint {}", sprintId);

        
        SprintResponse response = mapper.toSprintResponse(sprint);
        return attachMetrics(response, sprintId);
    }

    @Override
    @Transactional
    public SprintResponse completeSprint(Long sprintId, CompleteSprintRequest request, Integer userId) {
        log.info("Completing sprint {} with rollover={}, targetSprintId={}",
                 sprintId, request.rolloverIncompleteTasks(), request.targetSprintId());

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canEditProject(user, sprint.getProject())) {
            throw new UnauthorizedException("You don't have permission to manage sprints");
        }

        
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE sprints can be completed. Current status: " + sprint.getStatus());
        }

        
        List<Task> incompleteTasks = taskRepository.findIncompleteTasksBySprintId(sprintId);

        if (Boolean.TRUE.equals(request.rolloverIncompleteTasks()) && !incompleteTasks.isEmpty()) {
            if (request.targetSprintId() != null) {
                
                Sprint targetSprint = sprintRepository.findById(request.targetSprintId())
                        .orElseThrow(() -> new ResourceNotFoundException("Target Sprint", request.targetSprintId()));

                
                if (!targetSprint.getProject().getId().equals(sprint.getProject().getId())) {
                    throw new IllegalArgumentException("Target sprint must be in the same project");
                }

                
                if (targetSprint.getStatus() == SprintStatus.COMPLETED || targetSprint.getStatus() == SprintStatus.CANCELLED) {
                    throw new IllegalStateException("Cannot rollover to a completed or cancelled sprint");
                }

                log.info("Rolling over {} incomplete tasks to sprint {}", incompleteTasks.size(), request.targetSprintId());
                incompleteTasks.forEach(task -> task.setSprint(targetSprint));
            } else {
                
                log.info("Rolling over {} incomplete tasks to backlog", incompleteTasks.size());
                incompleteTasks.forEach(task -> task.setSprint(null));
            }
            taskRepository.saveAll(incompleteTasks);
        }
        

        
        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setCompletedAt(LocalDateTime.now());
        sprint.setCompletedBy(user);
        sprint = sprintRepository.save(sprint);

        log.info("Successfully completed sprint {}", sprintId);

        
        SprintResponse response = mapper.toSprintResponse(sprint);
        return attachMetrics(response, sprintId);
    }

    @Override
    @Transactional
    public void assignTasksToSprint(Long sprintId, List<Long> taskIds, Integer userId) {
        log.info("Assigning {} tasks to sprint {}", taskIds.size(), sprintId);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canEditProject(user, sprint.getProject())) {
            throw new UnauthorizedException("You don't have permission to manage sprints");
        }

        
        if (sprint.getStatus() == SprintStatus.COMPLETED || sprint.getStatus() == SprintStatus.CANCELLED) {
            throw new IllegalStateException("Cannot assign tasks to a completed or cancelled sprint");
        }

        
        List<Task> tasks = taskRepository.findAllById(taskIds);
        if (tasks.size() != taskIds.size()) {
            throw new ResourceNotFoundException("One or more tasks not found");
        }

        
        for (Task task : tasks) {
            if (!task.getProject().getId().equals(sprint.getProject().getId())) {
                throw new IllegalArgumentException("Task " + task.getKey() + " does not belong to the same project as the sprint");
            }
        }

        
        tasks.forEach(task -> task.setSprint(sprint));
        taskRepository.saveAll(tasks);

        log.info("Successfully assigned {} tasks to sprint {}", tasks.size(), sprintId);
    }

    @Override
    @Transactional
    public void removeTasksFromSprint(List<Long> taskIds, Integer userId) {
        log.info("Removing {} tasks from their sprints", taskIds.size());

        
        List<Task> tasks = taskRepository.findAllById(taskIds);
        if (tasks.isEmpty()) {
            throw new ResourceNotFoundException("No tasks found");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        for (Task task : tasks) {
            if (!permissionService.canEditProject(user, task.getProject())) {
                throw new UnauthorizedException("You don't have permission to modify tasks in project: " + task.getProject().getName());
            }
        }

        
        tasks.forEach(task -> task.setSprint(null));
        taskRepository.saveAll(tasks);

        log.info("Successfully removed {} tasks from sprints", tasks.size());
    }

    
    private SprintResponse attachMetrics(SprintResponse response, Long sprintId) {
        int totalTasks = taskRepository.countBySprintId(sprintId);
        int completedTasks = taskRepository.countCompletedBySprintId(sprintId);
        Double totalPoints = taskRepository.sumPointsBySprintId(sprintId);
        Double completedPoints = taskRepository.sumCompletedPointsBySprintId(sprintId);

        SprintMetrics metrics = SprintMetrics.calculate(
                totalTasks,
                completedTasks,
                totalPoints != null ? BigDecimal.valueOf(totalPoints) : BigDecimal.ZERO,
                completedPoints != null ? BigDecimal.valueOf(completedPoints) : BigDecimal.ZERO
        );

        return new SprintResponse(
                response.id(),
                response.projectId(),
                response.name(),
                response.goal(),
                response.startDate(),
                response.endDate(),
                response.status(),
                response.createdAt(),
                response.createdByUsername(),
                response.completedAt(),
                response.completedByUsername(),
                response.capacityHours(),
                metrics
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<?> getSprintTasks(Long sprintId, Integer requesterId) {
        log.info("Fetching tasks for sprint {}", sprintId);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        User user = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", requesterId));

        
        
        Project project = sprint.getProject();

        
        List<Task> tasks = taskRepository.findBySprintId(sprintId);

        
        return tasks.stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }
}
