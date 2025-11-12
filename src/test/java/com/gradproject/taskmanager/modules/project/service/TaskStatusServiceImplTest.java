package com.gradproject.taskmanager.modules.project.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.StatusCategory;
import com.gradproject.taskmanager.modules.project.domain.StatusTemplate;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.project.dto.ReorderStatusesRequest;
import com.gradproject.taskmanager.modules.project.dto.StatusTemplateResponse;
import com.gradproject.taskmanager.modules.project.dto.TaskStatusRequest;
import com.gradproject.taskmanager.modules.project.dto.TaskStatusResponse;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.project.repository.StatusTemplateRepository;
import com.gradproject.taskmanager.modules.project.repository.TaskStatusRepository;
import com.gradproject.taskmanager.shared.exception.DuplicateResourceException;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskStatusServiceImplTest {

    @Mock
    private TaskStatusRepository taskStatusRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private StatusTemplateRepository statusTemplateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMapper mapper;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private TaskStatusServiceImpl taskStatusService;

    private User testUser;
    private Organization testOrganization;
    private Project testProject;
    private TaskStatus testStatus;
    private TaskStatusRequest statusRequest;
    private TaskStatusResponse statusResponse;

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

        testStatus = new TaskStatus();
        testStatus.setId(1L);
        testStatus.setProject(testProject);
        testStatus.setName("To Do");
        testStatus.setColor("#6B7280");
        testStatus.setOrderIndex(0);
        testStatus.setCategory(StatusCategory.TODO);
        testStatus.setIsDefault(true);
        testStatus.setCreatedAt(LocalDateTime.now());

        statusRequest = new TaskStatusRequest("In Progress", "#3B82F6", StatusCategory.IN_PROGRESS);

        statusResponse = new TaskStatusResponse(
                1L,
                1L,
                "To Do",
                "#6B7280",
                0,
                StatusCategory.TODO,
                true,
                LocalDateTime.now()
        );
    }

    @Test
    void createStatus_shouldSucceed() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(taskStatusRepository.existsByProjectIdAndName(1L, "In Progress")).thenReturn(false);
        when(taskStatusRepository.findMaxOrderIndexByProjectId(1L)).thenReturn(0);
        when(taskStatusRepository.save(any(TaskStatus.class))).thenReturn(testStatus);
        when(mapper.toStatusResponse(testStatus)).thenReturn(statusResponse);

        
        TaskStatusResponse result = taskStatusService.createStatus(1L, statusRequest, 1);

        
        assertThat(result).isNotNull();
        verify(taskStatusRepository).save(argThat(status ->
                status.getName().equals("In Progress") &&
                status.getCategory() == StatusCategory.IN_PROGRESS &&
                status.getOrderIndex() == 1
        ));
    }

    @Test
    void createStatus_shouldThrowWhenProjectNotFound() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> taskStatusService.createStatus(1L, statusRequest, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project");
    }

    @Test
    void createStatus_shouldThrowWhenUnauthorized() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> taskStatusService.createStatus(1L, statusRequest, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void createStatus_shouldThrowWhenNameExists() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(taskStatusRepository.existsByProjectIdAndName(1L, "In Progress")).thenReturn(true);

        
        assertThatThrownBy(() -> taskStatusService.createStatus(1L, statusRequest, 1))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("name");
    }

    @Test
    void listProjectStatuses_shouldReturnStatusesInOrder() {
        
        TaskStatus status2 = new TaskStatus();
        status2.setId(2L);
        status2.setName("Done");
        status2.setOrderIndex(1);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(taskStatusRepository.findByProjectIdOrderByOrderIndexAsc(1L))
                .thenReturn(List.of(testStatus, status2));
        when(mapper.toStatusResponse(any(TaskStatus.class))).thenReturn(statusResponse);

        
        List<TaskStatusResponse> result = taskStatusService.listProjectStatuses(1L, 1);

        
        assertThat(result).hasSize(2);
        verify(mapper, times(2)).toStatusResponse(any(TaskStatus.class));
    }

    @Test
    void updateStatus_shouldSucceed() {
        
        when(taskStatusRepository.findById(1L)).thenReturn(Optional.of(testStatus));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(taskStatusRepository.existsByProjectIdAndNameExcludingId(1L, "In Progress", 1L))
                .thenReturn(false);
        when(taskStatusRepository.save(testStatus)).thenReturn(testStatus);
        when(mapper.toStatusResponse(testStatus)).thenReturn(statusResponse);

        
        TaskStatusResponse result = taskStatusService.updateStatus(1L, statusRequest, 1);

        
        assertThat(result).isNotNull();
        verify(taskStatusRepository).save(testStatus);
    }

    @Test
    void updateStatus_shouldThrowWhenStatusNotFound() {
        
        when(taskStatusRepository.findById(1L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> taskStatusService.updateStatus(1L, statusRequest, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Status");
    }

    @Test
    void deleteStatus_shouldSucceedWhenNotDefault() {
        
        testStatus.setIsDefault(false);
        when(taskStatusRepository.findById(1L)).thenReturn(Optional.of(testStatus));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);

        
        taskStatusService.deleteStatus(1L, 1);

        
        verify(taskStatusRepository).delete(testStatus);
    }

    @Test
    void deleteStatus_shouldAllowDeletingDefaultStatus() {
        
        
        testStatus.setIsDefault(true);
        when(taskStatusRepository.findById(1L)).thenReturn(Optional.of(testStatus));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);

        
        taskStatusService.deleteStatus(1L, 1);

        
        verify(taskStatusRepository).delete(testStatus);
    }

    @Test
    void reorderStatuses_shouldUpdateOrderIndices() {
        
        TaskStatus status2 = new TaskStatus();
        status2.setId(2L);
        status2.setProject(testProject);
        status2.setOrderIndex(1);

        TaskStatus status3 = new TaskStatus();
        status3.setId(3L);
        status3.setProject(testProject);
        status3.setOrderIndex(2);

        ReorderStatusesRequest reorderRequest = new ReorderStatusesRequest(List.of(3L, 1L, 2L));

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(taskStatusRepository.findById(1L)).thenReturn(Optional.of(testStatus));
        when(taskStatusRepository.findById(2L)).thenReturn(Optional.of(status2));
        when(taskStatusRepository.findById(3L)).thenReturn(Optional.of(status3));
        when(taskStatusRepository.save(any(TaskStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskStatusRepository.findByProjectIdOrderByOrderIndexAsc(1L))
                .thenReturn(List.of(status3, testStatus, status2));
        when(mapper.toStatusResponse(any(TaskStatus.class))).thenReturn(statusResponse);

        
        List<TaskStatusResponse> result = taskStatusService.reorderStatuses(1L, reorderRequest, 1);

        
        assertThat(result).hasSize(3);
        verify(taskStatusRepository, times(3)).save(any(TaskStatus.class));
    }

    @Test
    void reorderStatuses_shouldThrowWhenStatusNotInProject() {
        
        Project otherProject = new Project();
        otherProject.setId(2L);

        TaskStatus status2 = new TaskStatus();
        status2.setId(2L);
        status2.setProject(otherProject);

        ReorderStatusesRequest reorderRequest = new ReorderStatusesRequest(List.of(1L, 2L));

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(taskStatusRepository.findById(1L)).thenReturn(Optional.of(testStatus));
        when(taskStatusRepository.findById(2L)).thenReturn(Optional.of(status2));

        
        assertThatThrownBy(() -> taskStatusService.reorderStatuses(1L, reorderRequest, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to project");
    }

    @Test
    void getStatusTemplates_shouldReturnAllTemplates() {
        
        StatusTemplate template1 = new StatusTemplate();
        template1.setId(1L);
        template1.setName("Simple Kanban");
        template1.setCreatedAt(LocalDateTime.now());

        StatusTemplate template2 = new StatusTemplate();
        template2.setId(2L);
        template2.setName("Software Development");
        template2.setCreatedAt(LocalDateTime.now());

        when(statusTemplateRepository.findAllOrderByName()).thenReturn(List.of(template1, template2));
        when(mapper.toTemplateResponse(any(StatusTemplate.class)))
                .thenReturn(new StatusTemplateResponse(1L, "Simple Kanban", "Simple Kanban workflow", Collections.emptyList(), LocalDateTime.now()));

        
        List<StatusTemplateResponse> result = taskStatusService.getStatusTemplates();

        
        assertThat(result).hasSize(2);
        verify(mapper, times(2)).toTemplateResponse(any(StatusTemplate.class));
    }
}
