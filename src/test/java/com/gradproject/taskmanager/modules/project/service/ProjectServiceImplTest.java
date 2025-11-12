package com.gradproject.taskmanager.modules.project.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationMember;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationRole;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationMemberRepository;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationRepository;
import com.gradproject.taskmanager.modules.project.domain.*;
import com.gradproject.taskmanager.modules.project.dto.ProjectCreateRequest;
import com.gradproject.taskmanager.modules.project.dto.ProjectResponse;
import com.gradproject.taskmanager.modules.project.dto.ProjectUpdateRequest;
import com.gradproject.taskmanager.modules.project.repository.ProjectMemberRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private TaskStatusRepository taskStatusRepository;

    @Mock
    private StatusTemplateRepository statusTemplateRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMapper mapper;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private User testUser;
    private Organization testOrganization;
    private Project testProject;
    private ProjectCreateRequest createRequest;
    private ProjectUpdateRequest updateRequest;
    private ProjectResponse projectResponse;
    private OrganizationMember orgMember;
    private StatusTemplate statusTemplate;

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
        testProject.setDescription("Test Description");
        testProject.setOrganization(testOrganization);
        testProject.setCreatedBy(testUser);
        testProject.setCreatedAt(LocalDateTime.now());

        createRequest = new ProjectCreateRequest("TEST", "Test Project", "Test Description", null);
        updateRequest = new ProjectUpdateRequest("Updated Project", "Updated Description");

        projectResponse = new ProjectResponse(
                1L,
                1L,
                "TEST",
                "Test Project",
                "Test Description",
                null,
                LocalDateTime.now(),
                "testuser",
                0L,
                0L
        );

        orgMember = new OrganizationMember();
        orgMember.setUser(testUser);
        orgMember.setOrganization(testOrganization);
        orgMember.setRole(OrganizationRole.ORG_ADMIN);

        
        Map<String, Object> status1 = new HashMap<>();
        status1.put("name", "To Do");
        status1.put("color", "#6B7280");
        status1.put("category", "TODO");
        status1.put("order", 0);

        Map<String, Object> status2 = new HashMap<>();
        status2.put("name", "In Progress");
        status2.put("color", "#3B82F6");
        status2.put("category", "IN_PROGRESS");
        status2.put("order", 1);

        Map<String, Object> status3 = new HashMap<>();
        status3.put("name", "Done");
        status3.put("color", "#10B981");
        status3.put("category", "DONE");
        status3.put("order", 2);

        statusTemplate = new StatusTemplate();
        statusTemplate.setId(1L);
        statusTemplate.setName("Simple Kanban");
        statusTemplate.setStatuses(List.of(status1, status2, status3));
    }

    @Test
    void createProject_shouldSucceedWithTemplate() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(permissionService.isOrgOwnerOrAdmin(testUser, 1L)).thenReturn(true);
        when(projectRepository.existsByOrganizationIdAndKey(1L, "TEST")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(statusTemplateRepository.findById(1L)).thenReturn(Optional.of(statusTemplate));
        when(taskStatusRepository.save(any(TaskStatus.class))).thenAnswer(invocation -> {
            TaskStatus status = invocation.getArgument(0);
            status.setId(1L);
            return status;
        });
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(new ProjectMember());
        when(mapper.toResponse(any(Project.class))).thenReturn(projectResponse);

        ProjectCreateRequest requestWithTemplate = new ProjectCreateRequest("TEST", "Test Project", "Test Description", 1L);

        
        ProjectResponse result = projectService.createProject(1L, requestWithTemplate, 1);

        
        assertThat(result).isNotNull();
        verify(projectRepository, atLeast(1)).save(any(Project.class));
        verify(statusTemplateRepository).findById(1L);
        verify(taskStatusRepository, times(3)).save(any(TaskStatus.class));
        verify(projectMemberRepository).save(argThat(member ->
                member.getRole() == ProjectRole.PROJECT_OWNER &&
                member.getUser().equals(testUser)
        ));
    }

    @Test
    void createProject_shouldSucceedWithDefaultTemplate() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(permissionService.isOrgOwnerOrAdmin(testUser, 1L)).thenReturn(true);
        when(projectRepository.existsByOrganizationIdAndKey(1L, "TEST")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(statusTemplateRepository.findByName("Simple Kanban")).thenReturn(Optional.of(statusTemplate));
        when(statusTemplateRepository.findById(1L)).thenReturn(Optional.of(statusTemplate));
        when(taskStatusRepository.save(any(TaskStatus.class))).thenAnswer(invocation -> {
            TaskStatus status = invocation.getArgument(0);
            status.setId(1L);
            return status;
        });
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(new ProjectMember());
        when(mapper.toResponse(any(Project.class))).thenReturn(projectResponse);

        
        ProjectResponse result = projectService.createProject(1L, createRequest, 1);

        
        assertThat(result).isNotNull();
        verify(statusTemplateRepository).findByName("Simple Kanban");
        verify(statusTemplateRepository).findById(1L);
        verify(taskStatusRepository, times(3)).save(any(TaskStatus.class));
    }

    @Test
    void createProject_shouldThrowWhenUserNotFound() {
        
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> projectService.createProject(1L, createRequest, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void createProject_shouldThrowWhenOrganizationNotFound() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> projectService.createProject(1L, createRequest, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organization");
    }

    @Test
    void createProject_shouldThrowWhenUserNotOrgOwnerOrAdmin() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(permissionService.isOrgOwnerOrAdmin(testUser, 1L)).thenReturn(false);

        
        assertThatThrownBy(() -> projectService.createProject(1L, createRequest, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("owners and admins");
    }

    @Test
    void createProject_shouldThrowWhenProjectKeyExists() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(permissionService.isOrgOwnerOrAdmin(testUser, 1L)).thenReturn(true);
        when(projectRepository.existsByOrganizationIdAndKey(1L, "TEST")).thenReturn(true);

        
        assertThatThrownBy(() -> projectService.createProject(1L, createRequest, 1))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("key");
    }

    @Test
    void updateProject_shouldSucceedForProjectAdmin() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(true);
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(mapper.toResponse(testProject)).thenReturn(projectResponse);

        
        ProjectResponse result = projectService.updateProject(1L, updateRequest, 1);

        
        assertThat(result).isNotNull();
        verify(projectRepository).save(testProject);
    }

    @Test
    void updateProject_shouldThrowWhenUnauthorized() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canEditProject(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> projectService.updateProject(1L, updateRequest, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void deleteProject_shouldSucceedForProjectOwner() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canDeleteProject(testUser, testProject)).thenReturn(true);

        
        projectService.deleteProject(1L, 1);

        
        verify(projectRepository).delete(testProject);
    }

    @Test
    void deleteProject_shouldThrowForNonOwner() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canDeleteProject(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> projectService.deleteProject(1L, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("owners");
    }

    @Test
    void getProject_shouldSucceedForMember() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(mapper.toResponse(testProject)).thenReturn(projectResponse);

        
        ProjectResponse result = projectService.getProject(1L, 1);

        
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Project");
    }

    @Test
    void getProject_shouldThrowWhenNotMember() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> projectService.getProject(1L, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("access");
    }

    @Test
    void listOrganizationProjects_shouldReturnAccessibleProjects() {
        
        Project project2 = new Project();
        project2.setId(2L);
        project2.setOrganization(testOrganization);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(projectRepository.findByOrganizationId(1L)).thenReturn(List.of(testProject, project2));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(permissionService.canAccessProject(testUser, project2)).thenReturn(false);
        when(mapper.toResponse(testProject)).thenReturn(projectResponse);

        
        List<ProjectResponse> result = projectService.listOrganizationProjects(1L, 1);

        
        assertThat(result).hasSize(1);
        verify(mapper, times(1)).toResponse(any(Project.class));
    }

    @Test
    void listUserProjects_shouldReturnUserProjects() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(projectRepository.findByUserId(1)).thenReturn(List.of(testProject));
        when(mapper.toResponse(testProject)).thenReturn(projectResponse);

        
        List<ProjectResponse> result = projectService.listUserProjects(1);

        
        assertThat(result).hasSize(1);
        verify(mapper).toResponse(testProject);
    }
}
