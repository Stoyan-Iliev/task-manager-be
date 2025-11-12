package com.gradproject.taskmanager.modules.project.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.Sprint;
import com.gradproject.taskmanager.modules.project.domain.SprintStatus;
import com.gradproject.taskmanager.modules.project.dto.CompleteSprintRequest;
import com.gradproject.taskmanager.modules.project.dto.SprintRequest;
import com.gradproject.taskmanager.modules.project.dto.SprintResponse;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.project.repository.SprintRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.ProjectMapper;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceImplTest {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectMapper mapper;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private SprintServiceImpl sprintService;

    private User testUser;
    private Organization testOrganization;
    private Project testProject;
    private Sprint testSprint;
    private SprintRequest sprintRequest;
    private SprintResponse sprintResponse;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1);

        testOrganization = new Organization("Test Org", "test-org", "Test Description", testUser);
        testOrganization.setId(1L);

        testProject = new Project();
        testProject.setId(1L);
        testProject.setKey("TEST");
        testProject.setName("Test Project");
        testProject.setOrganization(testOrganization);
        testProject.setCreatedBy(testUser);

        testSprint = new Sprint();
        testSprint.setId(1L);
        testSprint.setProject(testProject);
        testSprint.setName("Sprint 1");
        testSprint.setGoal("Complete features");
        testSprint.setStartDate(LocalDate.now());
        testSprint.setEndDate(LocalDate.now().plusWeeks(2));
        testSprint.setStatus(SprintStatus.PLANNED);
        testSprint.setCreatedAt(LocalDateTime.now());

        sprintRequest = new SprintRequest(
                "Sprint 2",
                "New sprint goal",
                LocalDate.now().plusWeeks(2),
                LocalDate.now().plusWeeks(4)
        );

        sprintResponse = new SprintResponse(
                1L,
                1L,
                "Sprint 1",
                "Complete features",
                LocalDate.now(),
                LocalDate.now().plusWeeks(2),
                SprintStatus.PLANNED,
                LocalDateTime.now(),
                "testuser",
                null,  
                null,  
                null,  
                null   
        );
    }

    @Test
    void createSprint_shouldSucceed() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(sprintRepository.save(any(Sprint.class))).thenReturn(testSprint);
        when(mapper.toSprintResponse(testSprint)).thenReturn(sprintResponse);

        
        SprintResponse result = sprintService.createSprint(1L, sprintRequest, 1);

        
        assertThat(result).isNotNull();
        verify(sprintRepository).save(argThat(sprint ->
                sprint.getName().equals("Sprint 2") &&
                sprint.getGoal().equals("New sprint goal") &&
                sprint.getStatus() == SprintStatus.PLANNED
        ));
    }

    @Test
    void createSprint_shouldThrowWhenProjectNotFound() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> sprintService.createSprint(1L, sprintRequest, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project");
    }

    @Test
    void createSprint_shouldThrowWhenUnauthorized() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> sprintService.createSprint(1L, sprintRequest, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void createSprint_shouldThrowWhenEndDateBeforeStartDate() {
        
        SprintRequest invalidRequest = new SprintRequest(
                "Sprint 2",
                "Goal",
                LocalDate.now().plusWeeks(2),
                LocalDate.now()  
        );

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);

        
        assertThatThrownBy(() -> sprintService.createSprint(1L, invalidRequest, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be after start date");
    }

    @Test
    void listProjectSprints_shouldReturnSprintsInOrder() {
        
        Sprint sprint2 = new Sprint();
        sprint2.setId(2L);
        sprint2.setName("Sprint 2");
        sprint2.setStartDate(LocalDate.now().plusWeeks(2));

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(sprintRepository.findByProjectIdOrderByStartDateDesc(1L))
                .thenReturn(List.of(sprint2, testSprint));
        when(mapper.toSprintResponse(any(Sprint.class))).thenReturn(sprintResponse);

        
        List<SprintResponse> result = sprintService.listProjectSprints(1L, 1);

        
        assertThat(result).hasSize(2);
        verify(mapper, times(2)).toSprintResponse(any(Sprint.class));
    }

    @Test
    void getSprint_shouldSucceedForMember() {
        
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(mapper.toSprintResponse(testSprint)).thenReturn(sprintResponse);

        
        SprintResponse result = sprintService.getSprint(1L, 1);

        
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Sprint 1");
    }

    @Test
    void getSprint_shouldThrowWhenSprintNotFound() {
        
        when(sprintRepository.findById(1L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> sprintService.getSprint(1L, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sprint");
    }

    @Test
    void getSprint_shouldThrowWhenNotMember() {
        
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> sprintService.getSprint(1L, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("access");
    }

    @Test
    void updateSprint_shouldSucceed() {
        
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(sprintRepository.save(testSprint)).thenReturn(testSprint);
        when(mapper.toSprintResponse(testSprint)).thenReturn(sprintResponse);

        
        SprintResponse result = sprintService.updateSprint(1L, sprintRequest, 1);

        
        assertThat(result).isNotNull();
        verify(sprintRepository).save(testSprint);
    }

    @Test
    void updateSprint_shouldThrowWhenUnauthorized() {
        
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> sprintService.updateSprint(1L, sprintRequest, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void deleteSprint_shouldSucceed() {
        
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);

        
        sprintService.deleteSprint(1L, 1);

        
        verify(sprintRepository).delete(testSprint);
    }

    @Test
    void deleteSprint_shouldThrowWhenUnauthorized() {
        
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> sprintService.deleteSprint(1L, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permission");
    }

    

    @Test
    void startSprint_shouldSucceed() {
        
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(sprintRepository.countActiveSprintsByProject(1L)).thenReturn(0L);
        when(sprintRepository.save(testSprint)).thenReturn(testSprint);
        when(mapper.toSprintResponse(testSprint)).thenReturn(sprintResponse);
        when(taskRepository.countBySprintId(1L)).thenReturn(0);
        when(taskRepository.countCompletedBySprintId(1L)).thenReturn(0);
        when(taskRepository.sumPointsBySprintId(1L)).thenReturn(0.0);
        when(taskRepository.sumCompletedPointsBySprintId(1L)).thenReturn(0.0);

        
        SprintResponse result = sprintService.startSprint(1L, 1);

        
        assertThat(result).isNotNull();
        assertThat(testSprint.getStatus()).isEqualTo(SprintStatus.ACTIVE);
        verify(sprintRepository).save(testSprint);
    }

    @Test
    void startSprint_shouldThrowWhenNotPlanned() {
        
        testSprint.setStatus(SprintStatus.ACTIVE);
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);

        
        assertThatThrownBy(() -> sprintService.startSprint(1L, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PLANNED sprints can be started");
    }

    @Test
    void startSprint_shouldThrowWhenAnotherSprintActive() {
        
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(sprintRepository.countActiveSprintsByProject(1L)).thenReturn(1L);

        
        assertThatThrownBy(() -> sprintService.startSprint(1L, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Another sprint is already active");
    }

    @Test
    void completeSprint_shouldSucceedWithRolloverToBacklog() {
        
        testSprint.setStatus(SprintStatus.ACTIVE);
        CompleteSprintRequest request = CompleteSprintRequest.rolloverToBacklog();

        Task incompleteTask = new Task();
        incompleteTask.setId(1L);
        incompleteTask.setSprint(testSprint);

        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(taskRepository.findIncompleteTasksBySprintId(1L)).thenReturn(List.of(incompleteTask));
        when(taskRepository.saveAll(any())).thenReturn(List.of(incompleteTask));
        when(sprintRepository.save(testSprint)).thenReturn(testSprint);
        when(mapper.toSprintResponse(testSprint)).thenReturn(sprintResponse);
        when(taskRepository.countBySprintId(1L)).thenReturn(5);
        when(taskRepository.countCompletedBySprintId(1L)).thenReturn(3);
        when(taskRepository.sumPointsBySprintId(1L)).thenReturn(50.0);
        when(taskRepository.sumCompletedPointsBySprintId(1L)).thenReturn(30.0);

        
        SprintResponse result = sprintService.completeSprint(1L, request, 1);

        
        assertThat(result).isNotNull();
        assertThat(testSprint.getStatus()).isEqualTo(SprintStatus.COMPLETED);
        assertThat(testSprint.getCompletedBy()).isEqualTo(testUser);
        assertThat(testSprint.getCompletedAt()).isNotNull();
        assertThat(incompleteTask.getSprint()).isNull();
        verify(taskRepository).saveAll(any());
        verify(sprintRepository).save(testSprint);
    }

    @Test
    void completeSprint_shouldSucceedWithRolloverToAnotherSprint() {
        
        testSprint.setStatus(SprintStatus.ACTIVE);

        Sprint targetSprint = new Sprint();
        targetSprint.setId(2L);
        targetSprint.setProject(testProject);
        targetSprint.setStatus(SprintStatus.PLANNED);

        CompleteSprintRequest request = CompleteSprintRequest.rolloverToSprint(2L);

        Task incompleteTask = new Task();
        incompleteTask.setId(1L);
        incompleteTask.setSprint(testSprint);

        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(sprintRepository.findById(2L)).thenReturn(Optional.of(targetSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(taskRepository.findIncompleteTasksBySprintId(1L)).thenReturn(List.of(incompleteTask));
        when(taskRepository.saveAll(any())).thenReturn(List.of(incompleteTask));
        when(sprintRepository.save(testSprint)).thenReturn(testSprint);
        when(mapper.toSprintResponse(testSprint)).thenReturn(sprintResponse);
        when(taskRepository.countBySprintId(1L)).thenReturn(5);
        when(taskRepository.countCompletedBySprintId(1L)).thenReturn(5);
        when(taskRepository.sumPointsBySprintId(1L)).thenReturn(50.0);
        when(taskRepository.sumCompletedPointsBySprintId(1L)).thenReturn(50.0);

        
        SprintResponse result = sprintService.completeSprint(1L, request, 1);

        
        assertThat(result).isNotNull();
        assertThat(testSprint.getStatus()).isEqualTo(SprintStatus.COMPLETED);
        assertThat(incompleteTask.getSprint()).isEqualTo(targetSprint);
        verify(taskRepository).saveAll(any());
    }

    @Test
    void completeSprint_shouldThrowWhenNotActive() {
        
        testSprint.setStatus(SprintStatus.PLANNED);
        CompleteSprintRequest request = CompleteSprintRequest.rolloverToBacklog();

        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);

        
        assertThatThrownBy(() -> sprintService.completeSprint(1L, request, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only ACTIVE sprints can be completed");
    }

    @Test
    void assignTasksToSprint_shouldSucceed() {
        
        testSprint.setStatus(SprintStatus.PLANNED);

        Task task1 = new Task();
        task1.setId(1L);
        task1.setProject(testProject);

        Task task2 = new Task();
        task2.setId(2L);
        task2.setProject(testProject);

        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(taskRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(task1, task2));
        when(taskRepository.saveAll(any())).thenReturn(List.of(task1, task2));

        
        sprintService.assignTasksToSprint(1L, List.of(1L, 2L), 1);

        
        assertThat(task1.getSprint()).isEqualTo(testSprint);
        assertThat(task2.getSprint()).isEqualTo(testSprint);
        verify(taskRepository).saveAll(any());
    }

    @Test
    void assignTasksToSprint_shouldThrowWhenSprintCompleted() {
        
        testSprint.setStatus(SprintStatus.COMPLETED);

        when(sprintRepository.findById(1L)).thenReturn(Optional.of(testSprint));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);

        
        assertThatThrownBy(() -> sprintService.assignTasksToSprint(1L, List.of(1L), 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot assign tasks to a completed or cancelled sprint");
    }

    @Test
    void removeTasksFromSprint_shouldSucceed() {
        
        Task task1 = new Task();
        task1.setId(1L);
        task1.setProject(testProject);
        task1.setSprint(testSprint);

        when(taskRepository.findAllById(List.of(1L))).thenReturn(List.of(task1));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(taskRepository.saveAll(any())).thenReturn(List.of(task1));

        
        sprintService.removeTasksFromSprint(List.of(1L), 1);

        
        assertThat(task1.getSprint()).isNull();
        verify(taskRepository).saveAll(any());
    }

    @Test
    void removeTasksFromSprint_shouldThrowWhenUnauthorized() {
        
        Task task1 = new Task();
        task1.setId(1L);
        task1.setProject(testProject);

        when(taskRepository.findAllById(List.of(1L))).thenReturn(List.of(task1));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> sprintService.removeTasksFromSprint(List.of(1L), 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permission");
    }
}
