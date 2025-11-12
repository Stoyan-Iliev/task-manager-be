package com.gradproject.taskmanager.modules.task.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationMember;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationRole;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationMemberRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.Sprint;
import com.gradproject.taskmanager.modules.project.domain.StatusCategory;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.project.repository.SprintRepository;
import com.gradproject.taskmanager.modules.project.repository.TaskStatusRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import com.gradproject.taskmanager.modules.task.domain.TaskType;
import com.gradproject.taskmanager.modules.task.dto.*;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.BusinessRuleViolationException;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.TaskMapper;
import com.gradproject.taskmanager.shared.security.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskStatusRepository statusRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private TaskMapper mapper;

    @Mock
    private com.gradproject.taskmanager.modules.activity.service.ActivityLogService activityLogService;

    @Mock
    private TaskWatcherService watcherService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User user;
    private Organization organization;
    private Project project;
    private TaskStatus status;
    private Task task;

    @BeforeEach
    void setUp() {
        
        user = new User();
        user.setId(1);
        user.setUsername("testuser");

        organization = new Organization();
        organization.setId(1L);

        project = new Project();
        project.setId(1L);
        project.setKey("TEST");
        project.setOrganization(organization);

        status = new TaskStatus();
        status.setId(1L);
        status.setName("To Do");
        status.setCategory(StatusCategory.TODO);
        status.setProject(project);

        task = new Task();
        task.setId(1L);
        task.setKey("TEST-1");
        task.setTitle("Test Task");
        task.setProject(project);
        task.setOrganization(organization);
        task.setStatus(status);
        task.setReporter(user);
        task.setType(TaskType.TASK);
        task.setPriority(TaskPriority.MEDIUM);
    }

    @Test
    void createTask_success() {
        
        TaskCreateRequest request = new TaskCreateRequest(
                "New Task", "Description", 1L, null, null, null,
                TaskType.TASK, TaskPriority.HIGH, null, null, null
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(statusRepository.findById(1L)).thenReturn(Optional.of(status));
        when(mapper.fromCreateRequest(request)).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(mapper.toResponse(task)).thenReturn(createTaskResponse());
        when(taskRepository.countSubtasks(1L)).thenReturn(0L);

        
        TaskResponse response = taskService.createTask(1L, request, 1);

        
        assertThat(response).isNotNull();
        verify(taskRepository).save(any(Task.class));
        verify(permissionService).canManageTasks(user, project);
    }

    @Test
    void createTask_withoutPermission_throwsUnauthorizedException() {
        
        TaskCreateRequest request = new TaskCreateRequest(
                "New Task", null, 1L, null, null, null,
                TaskType.TASK, TaskPriority.MEDIUM, null, null, null
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.canManageTasks(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> taskService.createTask(1L, request, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("do not have permission");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_withInvalidStatus_throwsResourceNotFoundException() {
        
        TaskCreateRequest request = new TaskCreateRequest(
                "New Task", null, 999L, null, null, null,
                TaskType.TASK, TaskPriority.MEDIUM, null, null, null
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(statusRepository.findById(999L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> taskService.createTask(1L, request, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateTask_success() {
        
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated Title", "Updated Description",
                TaskPriority.HIGHEST, null, null, null
        );

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(taskRepository.save(task)).thenReturn(task);
        when(mapper.toResponse(task)).thenReturn(createTaskResponse());
        when(taskRepository.countSubtasks(1L)).thenReturn(0L);

        
        TaskResponse response = taskService.updateTask(1L, request, 1);

        
        assertThat(response).isNotNull();
        verify(mapper).updateFromRequest(request, task);
        verify(taskRepository).save(task);
    }

    @Test
    void deleteTask_success() {
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(taskRepository.countSubtasks(1L)).thenReturn(0L);

        
        taskService.deleteTask(1L, 1);

        
        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_withSubtasks_throwsBusinessRuleViolationException() {
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(taskRepository.countSubtasks(1L)).thenReturn(2L);

        
        assertThatThrownBy(() -> taskService.deleteTask(1L, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("subtask");

        verify(taskRepository, never()).delete(any());
    }

    @Test
    void getTask_success() {
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(mapper.toResponse(task)).thenReturn(createTaskResponse());
        when(taskRepository.countSubtasks(1L)).thenReturn(0L);

        
        TaskResponse response = taskService.getTask(1L, 1);

        
        assertThat(response).isNotNull();
        verify(permissionService).canAccessProject(user, project);
    }

    @Test
    void getTaskByKey_success() {
        
        OrganizationMember membership = new OrganizationMember(user, organization, OrganizationRole.ORG_MEMBER, user);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(membership));
        when(taskRepository.findByOrganizationIdAndKey(1L, "TEST-1")).thenReturn(Optional.of(task));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(mapper.toResponse(task)).thenReturn(createTaskResponse());
        when(taskRepository.countSubtasks(1L)).thenReturn(0L);

        
        TaskResponse response = taskService.getTaskByKey(1L, "TEST-1", 1);

        
        assertThat(response).isNotNull();
        assertThat(response.key()).isEqualTo("TEST-1");
    }

    @Test
    void listProjectTasks_success() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(taskRepository.findByProjectWithFilters(1L, null, null, null))
                .thenReturn(Arrays.asList(task));
        when(mapper.toSummary(task)).thenReturn(createTaskSummary());

        
        List<TaskSummary> tasks = taskService.listProjectTasks(1L, null, null, null, 1);

        
        assertThat(tasks).hasSize(1);
    }

    @Test
    void assignTask_success() {
        
        User assignee = new User();
        assignee.setId(2);
        TaskAssignRequest request = new TaskAssignRequest(2);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(organizationMemberRepository.existsByUserIdAndOrganizationId(2, 1L)).thenReturn(true);
        when(taskRepository.save(task)).thenReturn(task);
        when(mapper.toResponse(task)).thenReturn(createTaskResponse());
        when(taskRepository.countSubtasks(1L)).thenReturn(0L);

        
        TaskResponse response = taskService.assignTask(1L, request, 1);

        
        assertThat(response).isNotNull();
        verify(taskRepository).save(task);
    }

    @Test
    void unassignTask_success() {
        
        task.setAssignee(user);
        TaskAssignRequest request = new TaskAssignRequest(null);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(taskRepository.save(task)).thenReturn(task);
        when(mapper.toResponse(task)).thenReturn(createTaskResponse());
        when(taskRepository.countSubtasks(1L)).thenReturn(0L);

        
        TaskResponse response = taskService.assignTask(1L, request, 1);

        
        assertThat(response).isNotNull();
        verify(taskRepository).save(task);
    }

    @Test
    void transitionStatus_success() {
        
        TaskStatus newStatus = new TaskStatus();
        newStatus.setId(2L);
        newStatus.setName("In Progress");
        newStatus.setCategory(StatusCategory.IN_PROGRESS);
        newStatus.setProject(project);

        TaskTransitionRequest request = new TaskTransitionRequest(2L, "Starting work");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(statusRepository.findById(2L)).thenReturn(Optional.of(newStatus));
        when(taskRepository.save(task)).thenReturn(task);
        when(mapper.toResponse(task)).thenReturn(createTaskResponse());
        when(taskRepository.countSubtasks(1L)).thenReturn(0L);

        
        TaskResponse response = taskService.transitionStatus(1L, request, 1);

        
        assertThat(response).isNotNull();
        verify(taskRepository).save(task);
    }

    @Test
    void transitionStatus_invalidStatusForProject_throwsBusinessRuleViolationException() {
        
        Project otherProject = new Project();
        otherProject.setId(2L);

        TaskStatus wrongProjectStatus = new TaskStatus();
        wrongProjectStatus.setId(2L);
        wrongProjectStatus.setProject(otherProject);

        TaskTransitionRequest request = new TaskTransitionRequest(2L, null);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(statusRepository.findById(2L)).thenReturn(Optional.of(wrongProjectStatus));

        
        assertThatThrownBy(() -> taskService.transitionStatus(1L, request, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("does not belong to this project");
    }

    @Test
    void getMyOpenTasks_success() {
        
        OrganizationMember membership = new OrganizationMember(user, organization, OrganizationRole.ORG_MEMBER, user);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(membership));
        when(taskRepository.findMyOpenTasks(1, 1L)).thenReturn(Arrays.asList(task));
        when(mapper.toSummary(task)).thenReturn(createTaskSummary());

        
        List<TaskSummary> tasks = taskService.getMyOpenTasks(1L, 1);

        
        assertThat(tasks).hasSize(1);
    }

    @Test
    void getSubtasks_success() {
        
        Task subtask = new Task();
        subtask.setId(2L);
        subtask.setKey("TEST-2");
        subtask.setParentTask(task);
        subtask.setProject(project);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(taskRepository.findSubtasks(1L)).thenReturn(Arrays.asList(subtask));
        when(mapper.toSummary(subtask)).thenReturn(createTaskSummary());

        
        List<TaskSummary> subtasks = taskService.getSubtasks(1L, 1);

        
        assertThat(subtasks).hasSize(1);
    }

    @Test
    void createTask_withAssigneeNotInOrganization_throwsBusinessRuleViolationException() {
        
        User assignee = new User();
        assignee.setId(2);

        TaskCreateRequest request = new TaskCreateRequest(
                "New Task", null, 1L, 2, null, null,
                TaskType.TASK, TaskPriority.MEDIUM, null, null, null
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(statusRepository.findById(1L)).thenReturn(Optional.of(status));
        when(mapper.fromCreateRequest(request)).thenReturn(task);
        when(userRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(organizationMemberRepository.existsByUserIdAndOrganizationId(2, 1L)).thenReturn(false);

        
        assertThatThrownBy(() -> taskService.createTask(1L, request, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Assignee must be a member of the organization");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_withInvalidSprint_throwsResourceNotFoundException() {
        
        TaskCreateRequest request = new TaskCreateRequest(
                "New Task", null, 1L, null, 999L, null,
                TaskType.TASK, TaskPriority.MEDIUM, null, null, null
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(statusRepository.findById(1L)).thenReturn(Optional.of(status));
        when(mapper.fromCreateRequest(request)).thenReturn(task);
        when(sprintRepository.findById(999L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> taskService.createTask(1L, request, 1))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_withSprintFromDifferentProject_throwsBusinessRuleViolationException() {
        
        Project otherProject = new Project();
        otherProject.setId(2L);

        Sprint sprint = new Sprint();
        sprint.setId(1L);
        sprint.setProject(otherProject);

        TaskCreateRequest request = new TaskCreateRequest(
                "New Task", null, 1L, null, 1L, null,
                TaskType.TASK, TaskPriority.MEDIUM, null, null, null
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(statusRepository.findById(1L)).thenReturn(Optional.of(status));
        when(mapper.fromCreateRequest(request)).thenReturn(task);
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));

        
        assertThatThrownBy(() -> taskService.createTask(1L, request, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Sprint does not belong to this project");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_withInvalidParentTask_throwsResourceNotFoundException() {
        
        TaskCreateRequest request = new TaskCreateRequest(
                "New Task", null, 1L, null, null, 999L,
                TaskType.TASK, TaskPriority.MEDIUM, null, null, null
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(statusRepository.findById(1L)).thenReturn(Optional.of(status));
        when(mapper.fromCreateRequest(request)).thenReturn(task);
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> taskService.createTask(1L, request, 1))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_withParentTaskFromDifferentProject_throwsBusinessRuleViolationException() {
        
        Project otherProject = new Project();
        otherProject.setId(2L);

        Task parentTask = new Task();
        parentTask.setId(2L);
        parentTask.setProject(otherProject);

        TaskCreateRequest request = new TaskCreateRequest(
                "New Task", null, 1L, null, null, 2L,
                TaskType.TASK, TaskPriority.MEDIUM, null, null, null
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(statusRepository.findById(1L)).thenReturn(Optional.of(status));
        when(mapper.fromCreateRequest(request)).thenReturn(task);
        when(taskRepository.findById(2L)).thenReturn(Optional.of(parentTask));

        
        assertThatThrownBy(() -> taskService.createTask(1L, request, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Parent task must belong to the same project");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_withoutPermission_throwsUnauthorizedException() {
        
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated Title", null, null, null, null, null
        );

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> taskService.updateTask(1L, request, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("do not have permission to edit");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void getTask_withoutPermission_throwsUnauthorizedException() {
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> taskService.getTask(1L, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("do not have access to this task");
    }

    @Test
    void getTaskByKey_withoutOrganizationMembership_throwsUnauthorizedException() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> taskService.getTaskByKey(1L, "TEST-1", 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not a member of this organization");
    }

    @Test
    void getTaskByKey_withoutProjectAccess_throwsUnauthorizedException() {
        
        OrganizationMember membership = new OrganizationMember(user, organization, OrganizationRole.ORG_MEMBER, user);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(membership));
        when(taskRepository.findByOrganizationIdAndKey(1L, "TEST-1")).thenReturn(Optional.of(task));
        when(permissionService.canAccessProject(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> taskService.getTaskByKey(1L, "TEST-1", 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("do not have access to this task");
    }

    @Test
    void assignTask_withAssigneeNotInOrganization_throwsBusinessRuleViolationException() {
        
        User assignee = new User();
        assignee.setId(2);
        TaskAssignRequest request = new TaskAssignRequest(2);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(organizationMemberRepository.existsByUserIdAndOrganizationId(2, 1L)).thenReturn(false);

        
        assertThatThrownBy(() -> taskService.assignTask(1L, request, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Assignee must be a member of the organization");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void transitionStatus_withoutPermission_throwsUnauthorizedException() {
        
        TaskTransitionRequest request = new TaskTransitionRequest(2L, null);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> taskService.transitionStatus(1L, request, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("do not have permission to transition");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void getSubtasks_withoutPermission_throwsUnauthorizedException() {
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> taskService.getSubtasks(1L, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("do not have access to this task");
    }

    
    private TaskResponse createTaskResponse() {
        return new TaskResponse(
                1L, "TEST-1", "Test Task", "Description",
                new TaskStatusSummary(1L, "To Do", "#808080", "TODO"),
                null, null,
                TaskType.TASK, TaskPriority.MEDIUM,
                null, null, null, null,
                1L, "TEST", "Test Project",
                null, null, null, null,
                0, 0, 0, false, null, null, null
        );
    }

    private TaskSummary createTaskSummary() {
        return new TaskSummary(
                1L, "TEST-1", "Test Task",
                new TaskStatusSummary(1L, "To Do", "#808080", "TODO"),
                null, TaskPriority.MEDIUM, false
        );
    }
}
