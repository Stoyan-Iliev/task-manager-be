package com.gradproject.taskmanager.modules.analytics.service;

import com.gradproject.taskmanager.modules.analytics.dto.*;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.StatusCategory;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.release.domain.enums.ReleaseStatus;
import com.gradproject.taskmanager.modules.release.repository.ReleaseRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.security.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private User testUser;
    private Organization testOrganization;
    private Project testProject;
    private TaskStatus todoStatus;
    private TaskStatus inProgressStatus;
    private TaskStatus doneStatus;
    private List<Task> testTasks;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");
        testOrganization.setMembers(new HashSet<>());

        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Test Project");
        testProject.setOrganization(testOrganization);
        testProject.setMembers(new HashSet<>());

        todoStatus = new TaskStatus();
        todoStatus.setId(1L);
        todoStatus.setName("To Do");
        todoStatus.setCategory(StatusCategory.TODO);

        inProgressStatus = new TaskStatus();
        inProgressStatus.setId(2L);
        inProgressStatus.setName("In Progress");
        inProgressStatus.setCategory(StatusCategory.IN_PROGRESS);

        doneStatus = new TaskStatus();
        doneStatus.setId(3L);
        doneStatus.setName("Done");
        doneStatus.setCategory(StatusCategory.DONE);

        Task task1 = new Task();
        task1.setId(1L);
        task1.setTitle("Task 1");
        task1.setProject(testProject);
        task1.setStatus(todoStatus);

        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Task 2");
        task2.setProject(testProject);
        task2.setStatus(inProgressStatus);

        Task task3 = new Task();
        task3.setId(3L);
        task3.setTitle("Task 3");
        task3.setProject(testProject);
        task3.setStatus(doneStatus);

        testTasks = List.of(task1, task2, task3);
    }

    @Test
    void getProjectMetrics_success() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(taskRepository.findByProjectWithFilters(1L, null, null, null)).thenReturn(testTasks);
        when(taskRepository.findOverdueTasks(eq(1L), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(releaseRepository.findByProjectIdAndStatus(1L, ReleaseStatus.IN_PROGRESS)).thenReturn(Collections.emptyList());

        
        ProjectMetricsResponse response = analyticsService.getProjectMetrics(1L, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.projectName()).isEqualTo("Test Project");
        assertThat(response.totalTasks()).isEqualTo(3L);
        assertThat(response.completedTasks()).isEqualTo(1L);
        assertThat(response.inProgressTasks()).isEqualTo(1L);
        assertThat(response.todoTasks()).isEqualTo(1L);
        assertThat(response.completionRate()).isGreaterThan(0.0);

        verify(taskRepository).findByProjectWithFilters(1L, null, null, null);
        verify(permissionService).canAccessProject(testUser, testProject);
    }

    @Test
    void getProjectMetrics_userNotFound_throwsResourceNotFoundException() {
        
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> analyticsService.getProjectMetrics(1L, 999))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void getProjectMetrics_projectNotFound_throwsResourceNotFoundException() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> analyticsService.getProjectMetrics(999L, 1))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Project not found");
    }

    @Test
    void getProjectMetrics_unauthorizedUser_throwsUnauthorizedException() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> analyticsService.getProjectMetrics(1L, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("You don't have permission to view metrics");
    }

    @Test
    void getOrganizationProjectsMetrics_success() {
        
        Project project2 = new Project();
        project2.setId(2L);
        project2.setName("Project 2");
        project2.setOrganization(testOrganization);
        project2.setMembers(new HashSet<>());

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(projectRepository.findByOrganizationId(1L)).thenReturn(List.of(testProject, project2));
        when(permissionService.canAccessProject(eq(testUser), any(Project.class))).thenReturn(true);
        when(projectRepository.findById(anyLong())).thenReturn(Optional.of(testProject));
        when(taskRepository.findByProjectWithFilters(anyLong(), any(), any(), any())).thenReturn(testTasks);
        when(taskRepository.findOverdueTasks(anyLong(), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(releaseRepository.findByProjectIdAndStatus(anyLong(), any())).thenReturn(Collections.emptyList());

        
        List<ProjectMetricsResponse> responses = analyticsService.getOrganizationProjectsMetrics(1L, 1);

        
        assertThat(responses).hasSize(2);
        verify(projectRepository).findByOrganizationId(1L);
    }

    @Test
    void getOrganizationMetrics_success() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(projectRepository.findByOrganizationId(1L)).thenReturn(List.of(testProject));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(taskRepository.findByProjectWithFilters(1L, null, null, null)).thenReturn(testTasks);

        
        OrganizationMetricsResponse response = analyticsService.getOrganizationMetrics(1L, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.organizationId()).isEqualTo(1L);
        assertThat(response.organizationName()).isEqualTo("Test Org");
        assertThat(response.totalProjects()).isEqualTo(1L);
        assertThat(response.totalTasks()).isEqualTo(3L);
        assertThat(response.completedTasks()).isEqualTo(1L);

        verify(projectRepository).findByOrganizationId(1L);
    }

    @Test
    void getUserActivity_success() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(taskRepository.findMyOpenTasks(2, 1L)).thenReturn(testTasks);

        
        UserActivityResponse response = analyticsService.getUserActivity(2, 1L, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(2);
        assertThat(response.tasksAssigned()).isEqualTo(3L);
        assertThat(response.tasksCompleted()).isEqualTo(1L);

        verify(taskRepository).findMyOpenTasks(2, 1L);
    }

    @Test
    void getTaskStatusDistribution_success() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(taskRepository.findByProjectWithFilters(1L, null, null, null)).thenReturn(testTasks);

        
        TaskStatusDistributionResponse response = analyticsService.getTaskStatusDistribution(1L, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.statusDistribution()).isNotEmpty();
        assertThat(response.statusDistribution()).containsKeys("To Do", "In Progress", "Done");
        assertThat(response.statusDistribution().get("To Do")).isEqualTo(1L);
        assertThat(response.statusDistribution().get("In Progress")).isEqualTo(1L);
        assertThat(response.statusDistribution().get("Done")).isEqualTo(1L);
    }

    @Test
    void getTimeRangeMetrics_success() {
        
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

        
        TimeRangeMetricsResponse response = analyticsService.getTimeRangeMetrics(1L, startDate, endDate, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.startDate()).isEqualTo(startDate);
        assertThat(response.endDate()).isEqualTo(endDate);
        
        assertThat(response.tasksCreated()).isEqualTo(0L);
    }

    @Test
    void getProjectMetrics_emptyProject_returnsZeroMetrics() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(taskRepository.findByProjectWithFilters(1L, null, null, null)).thenReturn(Collections.emptyList());
        when(taskRepository.findOverdueTasks(eq(1L), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(releaseRepository.findByProjectIdAndStatus(1L, ReleaseStatus.IN_PROGRESS)).thenReturn(Collections.emptyList());

        
        ProjectMetricsResponse response = analyticsService.getProjectMetrics(1L, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.totalTasks()).isEqualTo(0L);
        assertThat(response.completedTasks()).isEqualTo(0L);
        assertThat(response.completionRate()).isEqualTo(0.0);
    }

    @Test
    void getOrganizationMetrics_filtersInaccessibleProjects() {
        
        Project project2 = new Project();
        project2.setId(2L);
        project2.setName("Inaccessible Project");
        project2.setOrganization(testOrganization);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(projectRepository.findByOrganizationId(1L)).thenReturn(List.of(testProject, project2));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(permissionService.canAccessProject(testUser, project2)).thenReturn(false);
        when(taskRepository.findByProjectWithFilters(1L, null, null, null)).thenReturn(testTasks);

        
        OrganizationMetricsResponse response = analyticsService.getOrganizationMetrics(1L, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.totalProjects()).isEqualTo(1L); 

        verify(permissionService).canAccessProject(testUser, testProject);
        verify(permissionService).canAccessProject(testUser, project2);
    }
}
