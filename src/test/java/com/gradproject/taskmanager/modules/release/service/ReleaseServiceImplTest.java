package com.gradproject.taskmanager.modules.release.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceImplTest {

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private ReleaseTaskRepository releaseTaskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ReleaseMapper releaseMapper;

    @InjectMocks
    private ReleaseServiceImpl releaseService;

    private User testUser;
    private Organization testOrganization;
    private Project testProject;
    private Release testRelease;
    private Task testTask;
    private TaskStatus testStatus;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Test Project");
        testProject.setOrganization(testOrganization);

        testStatus = new TaskStatus();
        testStatus.setId(1L);
        testStatus.setName("To Do");

        testTask = new Task();
        testTask.setId(1L);
        testTask.setTitle("Test Task");
        testTask.setProject(testProject);
        testTask.setStatus(testStatus);
        testTask.setOrganization(testOrganization);

        testRelease = new Release(testProject, "v1.0.0", "1.0.0", LocalDate.now().plusDays(30), testUser);
        testRelease.setId(1L);
        testRelease.setDescription("First release");
        testRelease.setStatus(ReleaseStatus.PLANNED);
    }

    @Test
    void createRelease_success() {
        
        CreateReleaseRequest request = new CreateReleaseRequest(
            "v1.0.0", "First release", "1.0.0", LocalDate.now().plusDays(30)
        );

        ReleaseResponse expectedResponse = new ReleaseResponse(
            1L, 1L, "Test Project", "v1.0.0", "First release", "1.0.0",
            LocalDate.now().plusDays(30), ReleaseStatus.PLANNED,
            1, "testuser", LocalDateTime.now(), null, null, null, 0L, 0L
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(releaseRepository.existsByProjectIdAndName(1L, "v1.0.0")).thenReturn(false);
        when(releaseRepository.save(any(Release.class))).thenReturn(testRelease);
        when(releaseMapper.toResponse(testRelease)).thenReturn(expectedResponse);
        when(releaseTaskRepository.countByReleaseId(1L)).thenReturn(0L);
        when(releaseTaskRepository.countCompletedByReleaseId(1L)).thenReturn(0L);

        
        ReleaseResponse response = releaseService.createRelease(1L, request, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("v1.0.0");

        verify(releaseRepository).save(any(Release.class));
        verify(permissionService).canEditProject(testUser, testProject);
    }

    @Test
    void createRelease_duplicateName_throwsDuplicateResourceException() {
        
        CreateReleaseRequest request = new CreateReleaseRequest(
            "v1.0.0", "First release", "1.0.0", LocalDate.now().plusDays(30)
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(releaseRepository.existsByProjectIdAndName(1L, "v1.0.0")).thenReturn(true);

        
        assertThatThrownBy(() -> releaseService.createRelease(1L, request, 1))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Release with name 'v1.0.0' already exists");
    }

    @Test
    void createRelease_userNotFound_throwsResourceNotFoundException() {
        
        CreateReleaseRequest request = new CreateReleaseRequest(
            "v1.0.0", "First release", "1.0.0", LocalDate.now().plusDays(30)
        );

        when(userRepository.findById(999)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> releaseService.createRelease(1L, request, 999))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void createRelease_unauthorizedUser_throwsUnauthorizedException() {
        
        CreateReleaseRequest request = new CreateReleaseRequest(
            "v1.0.0", "First release", "1.0.0", LocalDate.now().plusDays(30)
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> releaseService.createRelease(1L, request, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("You don't have permission to create releases");
    }

    @Test
    void getProjectReleases_success() {
        
        Release release2 = new Release(testProject, "v2.0.0", "2.0.0", LocalDate.now().plusDays(60), testUser);
        release2.setId(2L);

        ReleaseResponse response1 = new ReleaseResponse(
            1L, 1L, "Test Project", "v1.0.0", "First release", "1.0.0",
            LocalDate.now().plusDays(30), ReleaseStatus.PLANNED,
            1, "testuser", LocalDateTime.now(), null, null, null, 0L, 0L
        );
        ReleaseResponse response2 = new ReleaseResponse(
            2L, 1L, "Test Project", "v2.0.0", null, "2.0.0",
            LocalDate.now().plusDays(60), ReleaseStatus.PLANNED,
            1, "testuser", LocalDateTime.now(), null, null, null, 0L, 0L
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(releaseRepository.findByProjectId(1L)).thenReturn(List.of(testRelease, release2));
        when(releaseMapper.toResponse(testRelease)).thenReturn(response1);
        when(releaseMapper.toResponse(release2)).thenReturn(response2);
        when(releaseTaskRepository.countByReleaseId(anyLong())).thenReturn(0L);
        when(releaseTaskRepository.countCompletedByReleaseId(anyLong())).thenReturn(0L);

        
        List<ReleaseResponse> responses = releaseService.getProjectReleases(1L, 1);

        
        assertThat(responses).hasSize(2);
        verify(releaseRepository).findByProjectId(1L);
    }

    @Test
    void getRelease_success() {
        
        ReleaseResponse expectedResponse = new ReleaseResponse(
            1L, 1L, "Test Project", "v1.0.0", "First release", "1.0.0",
            LocalDate.now().plusDays(30), ReleaseStatus.PLANNED,
            1, "testuser", LocalDateTime.now(), null, null, null, 0L, 0L
        );

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(testRelease));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(releaseMapper.toResponse(testRelease)).thenReturn(expectedResponse);
        when(releaseTaskRepository.countByReleaseId(1L)).thenReturn(0L);
        when(releaseTaskRepository.countCompletedByReleaseId(1L)).thenReturn(0L);

        
        ReleaseResponse response = releaseService.getRelease(1L, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void updateRelease_success() {
        
        UpdateReleaseRequest request = new UpdateReleaseRequest(
            "v1.0.1", "Updated description", "1.0.1",
            LocalDate.now().plusDays(20), ReleaseStatus.IN_PROGRESS
        );

        ReleaseResponse expectedResponse = new ReleaseResponse(
            1L, 1L, "Test Project", "v1.0.1", "Updated description", "1.0.1",
            LocalDate.now().plusDays(20), ReleaseStatus.IN_PROGRESS,
            1, "testuser", LocalDateTime.now(), LocalDateTime.now(), null, null, 0L, 0L
        );

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(testRelease));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(releaseRepository.existsByProjectIdAndNameExcludingId(1L, "v1.0.1", 1L)).thenReturn(false);
        when(releaseRepository.save(testRelease)).thenReturn(testRelease);
        when(releaseMapper.toResponse(testRelease)).thenReturn(expectedResponse);
        when(releaseTaskRepository.countByReleaseId(1L)).thenReturn(0L);
        when(releaseTaskRepository.countCompletedByReleaseId(1L)).thenReturn(0L);

        
        ReleaseResponse response = releaseService.updateRelease(1L, request, 1);

        
        assertThat(response).isNotNull();
        verify(releaseRepository).save(testRelease);
    }

    @Test
    void updateRelease_duplicateName_throwsDuplicateResourceException() {
        
        UpdateReleaseRequest request = new UpdateReleaseRequest(
            "v2.0.0", null, null, null, null
        );

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(testRelease));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(releaseRepository.existsByProjectIdAndNameExcludingId(1L, "v2.0.0", 1L)).thenReturn(true);

        
        assertThatThrownBy(() -> releaseService.updateRelease(1L, request, 1))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Release with name 'v2.0.0' already exists");
    }

    @Test
    void deleteRelease_success() {
        
        when(releaseRepository.findById(1L)).thenReturn(Optional.of(testRelease));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);

        
        releaseService.deleteRelease(1L, 1);

        
        verify(releaseRepository).delete(testRelease);
    }

    @Test
    void addTaskToRelease_success() {
        
        ReleaseResponse expectedResponse = new ReleaseResponse(
            1L, 1L, "Test Project", "v1.0.0", "First release", "1.0.0",
            LocalDate.now().plusDays(30), ReleaseStatus.PLANNED,
            1, "testuser", LocalDateTime.now(), null, null, null, 1L, 0L
        );

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(testRelease));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageTasks(testUser, testProject)).thenReturn(true);
        when(releaseTaskRepository.existsByReleaseIdAndTaskId(1L, 1L)).thenReturn(false);
        when(releaseMapper.toResponse(testRelease)).thenReturn(expectedResponse);
        when(releaseTaskRepository.countByReleaseId(1L)).thenReturn(1L);
        when(releaseTaskRepository.countCompletedByReleaseId(1L)).thenReturn(0L);

        
        ReleaseResponse response = releaseService.addTaskToRelease(1L, 1L, 1);

        
        assertThat(response).isNotNull();
        verify(releaseTaskRepository).save(any(ReleaseTask.class));
    }

    @Test
    void addTaskToRelease_taskAlreadyInRelease_throwsDuplicateResourceException() {
        
        when(releaseRepository.findById(1L)).thenReturn(Optional.of(testRelease));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageTasks(testUser, testProject)).thenReturn(true);
        when(releaseTaskRepository.existsByReleaseIdAndTaskId(1L, 1L)).thenReturn(true);

        
        assertThatThrownBy(() -> releaseService.addTaskToRelease(1L, 1L, 1))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Task is already in this release");
    }

    @Test
    void addTaskToRelease_taskFromDifferentProject_throwsIllegalArgumentException() {
        
        Project anotherProject = new Project();
        anotherProject.setId(2L);
        anotherProject.setName("Another Project");
        testTask.setProject(anotherProject);

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(testRelease));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageTasks(testUser, testProject)).thenReturn(true);

        
        assertThatThrownBy(() -> releaseService.addTaskToRelease(1L, 1L, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task must belong to the same project");
    }

    @Test
    void removeTaskFromRelease_success() {
        
        ReleaseTask releaseTask = new ReleaseTask(testRelease, testTask, testUser);

        ReleaseResponse expectedResponse = new ReleaseResponse(
            1L, 1L, "Test Project", "v1.0.0", "First release", "1.0.0",
            LocalDate.now().plusDays(30), ReleaseStatus.PLANNED,
            1, "testuser", LocalDateTime.now(), null, null, null, 0L, 0L
        );

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(testRelease));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageTasks(testUser, testProject)).thenReturn(true);
        when(releaseTaskRepository.findByReleaseIdAndTaskId(1L, 1L)).thenReturn(Optional.of(releaseTask));
        when(releaseMapper.toResponse(testRelease)).thenReturn(expectedResponse);
        when(releaseTaskRepository.countByReleaseId(1L)).thenReturn(0L);
        when(releaseTaskRepository.countCompletedByReleaseId(1L)).thenReturn(0L);

        
        ReleaseResponse response = releaseService.removeTaskFromRelease(1L, 1L, 1);

        
        assertThat(response).isNotNull();
        verify(releaseTaskRepository).delete(releaseTask);
    }

    @Test
    void markReleaseAsReleased_success() {
        
        ReleaseResponse expectedResponse = new ReleaseResponse(
            1L, 1L, "Test Project", "v1.0.0", "First release", "1.0.0",
            LocalDate.now().plusDays(30), ReleaseStatus.RELEASED,
            1, "testuser", LocalDateTime.now(), LocalDateTime.now(),
            LocalDateTime.now(), null, 0L, 0L
        );

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(testRelease));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(releaseRepository.save(testRelease)).thenReturn(testRelease);
        when(releaseMapper.toResponse(testRelease)).thenReturn(expectedResponse);
        when(releaseTaskRepository.countByReleaseId(1L)).thenReturn(0L);
        when(releaseTaskRepository.countCompletedByReleaseId(1L)).thenReturn(0L);

        
        ReleaseResponse response = releaseService.markReleaseAsReleased(1L, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ReleaseStatus.RELEASED);
        verify(releaseRepository).save(testRelease);
    }

    @Test
    void archiveRelease_success() {
        
        ReleaseResponse expectedResponse = new ReleaseResponse(
            1L, 1L, "Test Project", "v1.0.0", "First release", "1.0.0",
            LocalDate.now().plusDays(30), ReleaseStatus.ARCHIVED,
            1, "testuser", LocalDateTime.now(), LocalDateTime.now(),
            null, LocalDateTime.now(), 0L, 0L
        );

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(testRelease));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(releaseRepository.save(testRelease)).thenReturn(testRelease);
        when(releaseMapper.toResponse(testRelease)).thenReturn(expectedResponse);
        when(releaseTaskRepository.countByReleaseId(1L)).thenReturn(0L);
        when(releaseTaskRepository.countCompletedByReleaseId(1L)).thenReturn(0L);

        
        ReleaseResponse response = releaseService.archiveRelease(1L, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ReleaseStatus.ARCHIVED);
        verify(releaseRepository).save(testRelease);
    }

    @Test
    void getReleaseTasks_success() {
        
        ReleaseTask releaseTask1 = new ReleaseTask(testRelease, testTask, testUser);
        releaseTask1.setId(1L);

        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Test Task 2");
        ReleaseTask releaseTask2 = new ReleaseTask(testRelease, task2, testUser);
        releaseTask2.setId(2L);

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(testRelease));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(releaseTaskRepository.findByReleaseId(1L)).thenReturn(List.of(releaseTask1, releaseTask2));

        
        List<Long> taskIds = releaseService.getReleaseTasks(1L, 1);

        
        assertThat(taskIds).hasSize(2);
        assertThat(taskIds).containsExactly(1L, 2L);
    }
}
