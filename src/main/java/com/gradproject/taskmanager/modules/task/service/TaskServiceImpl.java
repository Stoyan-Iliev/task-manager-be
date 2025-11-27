package com.gradproject.taskmanager.modules.task.service;

import com.gradproject.taskmanager.modules.activity.service.ActivityLogService;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.event.TaskAssignedEvent;
import com.gradproject.taskmanager.modules.notification.event.TaskCreatedEvent;
import com.gradproject.taskmanager.modules.notification.event.TaskStatusChangedEvent;
import com.gradproject.taskmanager.modules.notification.event.TaskUnassignedEvent;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationMember;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationMemberRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.Sprint;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.project.repository.SprintRepository;
import com.gradproject.taskmanager.modules.project.repository.TaskStatusRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.dto.*;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.BusinessRuleViolationException;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.TaskMapper;
import com.gradproject.taskmanager.shared.security.PermissionService;
import com.gradproject.taskmanager.shared.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskStatusRepository statusRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PermissionService permissionService;
    private final TaskMapper mapper;
    private final ActivityLogService activityLogService;
    private final TaskWatcherService watcherService;
    private final ApplicationEventPublisher eventPublisher;
    private final HtmlSanitizer htmlSanitizer;

    @Override
    @Transactional
    public TaskResponse createTask(Long projectId, TaskCreateRequest request, Integer userId) {
        log.debug("Creating task in project {} by user {}", projectId, userId);

        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        
        if (!permissionService.canManageTasks(user, project)) {
            throw new UnauthorizedException("You do not have permission to create tasks in this project");
        }

        
        TaskStatus status = statusRepository.findById(request.statusId())
                .orElseThrow(() -> new ResourceNotFoundException("TaskStatus", request.statusId()));

        if (!status.getProject().getId().equals(projectId)) {
            throw new BusinessRuleViolationException("Status does not belong to this project");
        }


        Task task = mapper.fromCreateRequest(request);
        task.setOrganization(project.getOrganization());
        task.setProject(project);
        task.setStatus(status);
        task.setReporter(user);
        task.setCreatedBy(user);

        // Sanitize HTML in description to prevent XSS
        if (task.getDescription() != null) {
            task.setDescription(htmlSanitizer.sanitize(task.getDescription()));
        }

        
        if (request.assigneeId() != null) {
            User assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.assigneeId()));

            
            if (!organizationMemberRepository.existsByUserIdAndOrganizationId(
                    assignee.getId(), project.getOrganization().getId())) {
                throw new BusinessRuleViolationException("Assignee must be a member of the organization");
            }

            task.setAssignee(assignee);
        }

        
        if (request.sprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.sprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint", request.sprintId()));

            if (!sprint.getProject().getId().equals(projectId)) {
                throw new BusinessRuleViolationException("Sprint does not belong to this project");
            }

            task.setSprint(sprint);
        }

        
        if (request.parentTaskId() != null) {
            Task parentTask = taskRepository.findById(request.parentTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task", request.parentTaskId()));

            if (!parentTask.getProject().getId().equals(projectId)) {
                throw new BusinessRuleViolationException("Parent task must belong to the same project");
            }

            task.setParentTask(parentTask);
        }

        
        task = taskRepository.save(task);

        
        activityLogService.logTaskCreated(task, user);

        
        watcherService.autoWatchOnCreate(task, user);

        
        if (task.getAssignee() != null) {
            watcherService.autoWatchOnAssign(task, task.getAssignee());
        }

        
        eventPublisher.publishEvent(new TaskCreatedEvent(this, task, user));

        
        if (task.getAssignee() != null) {
            eventPublisher.publishEvent(new TaskAssignedEvent(this, task, task.getAssignee(), user));
        }

        
        TaskResponse response = mapper.toResponse(task);

        
        long subtaskCount = task.getId() != null ? taskRepository.countSubtasks(task.getId()) : 0;

        return new TaskResponse(
            response.id(), response.key(), response.title(), response.description(),
            response.status(), response.assignee(), response.reporter(),
            response.type(), response.priority(), response.dueDate(),
            response.estimatedHours(), response.loggedHours(), response.storyPoints(),
            response.projectId(), response.projectKey(), response.projectName(),
            response.sprintId(), response.sprintName(),
            response.parentTaskId(), response.parentTaskKey(),
            (int) subtaskCount, 0, 0,  
            response.isOverdue(), response.createdAt(), response.updatedAt(), response.createdBy()
        );
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request, Integer userId) {
        log.debug("Updating task {} by user {}", taskId, userId);

        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canManageTasks(user, task.getProject())) {
            throw new UnauthorizedException("You do not have permission to edit this task");
        }


        mapper.updateFromRequest(request, task);
        task.setUpdatedBy(user);

        // Sanitize HTML in description to prevent XSS
        if (task.getDescription() != null) {
            task.setDescription(htmlSanitizer.sanitize(task.getDescription()));
        }

        task = taskRepository.save(task);

        return buildTaskResponse(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, Integer userId) {
        log.debug("Deleting task {} by user {}", taskId, userId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canManageTasks(user, task.getProject())) {
            throw new UnauthorizedException("You do not have permission to delete this task");
        }

        
        long subtaskCount = taskRepository.countSubtasks(taskId);
        if (subtaskCount > 0) {
            throw new BusinessRuleViolationException(
                "Cannot delete task with " + subtaskCount + " subtask(s). Delete or move subtasks first."
            );
        }

        taskRepository.delete(task);
        log.info("Deleted task {} from project {}", task.getKey(), task.getProject().getKey());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId, Integer userId) {
        log.debug("Getting task {} for user {}", taskId, userId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canAccessProject(user, task.getProject())) {
            throw new UnauthorizedException("You do not have access to this task");
        }

        return buildTaskResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskByKey(Long orgId, String key, Integer userId) {
        log.debug("Getting task {} in organization {} for user {}", key, orgId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        OrganizationMember membership = organizationMemberRepository
                .findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this organization"));

        Task task = taskRepository.findByOrganizationIdAndKey(orgId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Task with key '" + key + "' not found"));

        
        if (!permissionService.canAccessProject(user, task.getProject())) {
            throw new UnauthorizedException("You do not have access to this task");
        }

        return buildTaskResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listProjectTasks(Long projectId, Long statusId,
                                               Integer assigneeId, Long sprintId, Integer userId) {
        log.debug("Listing tasks in project {} with filters: status={}, assignee={}, sprint={}",
                  projectId, statusId, assigneeId, sprintId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));


        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You do not have access to this project");
        }


        List<Task> tasks = taskRepository.findByProjectWithFilters(projectId, statusId, assigneeId, sprintId);

        return tasks.stream()
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse assignTask(Long taskId, TaskAssignRequest request, Integer userId) {
        log.debug("Assigning task {} to user {} by user {}", taskId, request.assigneeId(), userId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canManageTasks(user, task.getProject())) {
            throw new UnauthorizedException("You do not have permission to assign this task");
        }

        
        User oldAssignee = task.getAssignee();
        User newAssignee = null;

        if (request.assigneeId() != null) {
            
            User assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.assigneeId()));

            
            if (!organizationMemberRepository.existsByUserIdAndOrganizationId(
                    assignee.getId(), task.getOrganization().getId())) {
                throw new BusinessRuleViolationException("Assignee must be a member of the organization");
            }

            newAssignee = assignee;
            task.setAssignee(assignee);
            log.info("Assigned task {} to user {}", task.getKey(), assignee.getUsername());
        } else {
            
            task.setAssignee(null);
            log.info("Unassigned task {}", task.getKey());
        }

        task.setUpdatedBy(user);
        task = taskRepository.save(task);

        
        activityLogService.logAssignment(task, oldAssignee, newAssignee, user);

        
        if (newAssignee != null) {
            watcherService.autoWatchOnAssign(task, newAssignee);
        }

        
        if (newAssignee != null) {
            eventPublisher.publishEvent(new TaskAssignedEvent(this, task, newAssignee, user));
        } else if (oldAssignee != null) {
            eventPublisher.publishEvent(new TaskUnassignedEvent(this, task, oldAssignee, user));
        }

        return buildTaskResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse transitionStatus(Long taskId, TaskTransitionRequest request, Integer userId) {
        log.debug("Transitioning task {} to status {} by user {}", taskId, request.newStatusId(), userId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canManageTasks(user, task.getProject())) {
            throw new UnauthorizedException("You do not have permission to transition this task");
        }

        
        TaskStatus newStatus = statusRepository.findById(request.newStatusId())
                .orElseThrow(() -> new ResourceNotFoundException("TaskStatus", request.newStatusId()));

        if (!newStatus.getProject().getId().equals(task.getProject().getId())) {
            throw new BusinessRuleViolationException("Status does not belong to this project");
        }

        
        TaskStatus oldStatus = task.getStatus();

        
        if (!task.canTransitionTo(newStatus)) {
            throw new BusinessRuleViolationException(
                "Cannot transition from " + task.getStatus().getName() + " to " + newStatus.getName()
            );
        }

        task.setStatus(newStatus);
        task.setUpdatedBy(user);
        task = taskRepository.save(task);

        
        activityLogService.logStatusChange(task, oldStatus, newStatus, user);

        
        eventPublisher.publishEvent(new TaskStatusChangedEvent(this, task, oldStatus, newStatus, user));

        
        log.info("Transitioned task {} to status {}", task.getKey(), newStatus.getName());

        return buildTaskResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskSummary> getMyOpenTasks(Long orgId, Integer userId) {
        log.debug("Getting open tasks for user {} in organization {}", userId, orgId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        OrganizationMember membership = organizationMemberRepository
                .findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this organization"));

        List<Task> tasks = taskRepository.findMyOpenTasks(userId, orgId);

        return tasks.stream()
                .map(mapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskSummary> getSubtasks(Long parentTaskId, Integer userId) {
        log.debug("Getting subtasks of task {} for user {}", parentTaskId, userId);

        Task parentTask = taskRepository.findById(parentTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", parentTaskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canAccessProject(user, parentTask.getProject())) {
            throw new UnauthorizedException("You do not have access to this task");
        }

        List<Task> subtasks = taskRepository.findSubtasks(parentTaskId);

        return subtasks.stream()
                .map(mapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskSummary> getBacklogTasks(Long projectId, Integer userId) {
        log.debug("Fetching backlog tasks for project {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canAccessProject(requester, project)) {
            throw new UnauthorizedException("You do not have access to this project");
        }

        List<Task> backlogTasks = taskRepository.findBacklogTasks(projectId);

        return backlogTasks.stream()
                .map(mapper::toSummary)
                .collect(Collectors.toList());
    }

    
    private TaskResponse buildTaskResponse(Task task) {
        TaskResponse response = mapper.toResponse(task);

        
        long subtaskCount = taskRepository.countSubtasks(task.getId());

        return new TaskResponse(
            response.id(), response.key(), response.title(), response.description(),
            response.status(), response.assignee(), response.reporter(),
            response.type(), response.priority(), response.dueDate(),
            response.estimatedHours(), response.loggedHours(), response.storyPoints(),
            response.projectId(), response.projectKey(), response.projectName(),
            response.sprintId(), response.sprintName(),
            response.parentTaskId(), response.parentTaskKey(),
            (int) subtaskCount, 0, 0,  
            response.isOverdue(), response.createdAt(), response.updatedAt(), response.createdBy()
        );
    }
}
