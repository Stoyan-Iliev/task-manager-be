package com.gradproject.taskmanager.shared.security;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationMember;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationRole;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationMemberRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.ProjectMember;
import com.gradproject.taskmanager.modules.project.domain.ProjectRole;
import com.gradproject.taskmanager.modules.project.repository.ProjectMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private PermissionService permissionService;

    private User testUser;
    private Organization testOrganization;
    private Project testProject;
    private OrganizationMember orgMember;
    private ProjectMember projectMember;

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

        orgMember = new OrganizationMember();
        orgMember.setUser(testUser);
        orgMember.setOrganization(testOrganization);

        projectMember = new ProjectMember();
        projectMember.setUser(testUser);
        projectMember.setProject(testProject);
    }

    @Test
    void getEffectiveProjectRole_shouldReturnNullWhenNotOrgMember() {
        
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.empty());

        
        ProjectRole result = permissionService.getEffectiveProjectRole(testUser, testProject);

        
        assertThat(result).isNull();
    }

    @Test
    void getEffectiveProjectRole_shouldReturnProjectAdminForOrgOwner() {
        
        orgMember.setRole(OrganizationRole.ORG_OWNER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.empty());

        
        ProjectRole result = permissionService.getEffectiveProjectRole(testUser, testProject);

        
        assertThat(result).isEqualTo(ProjectRole.PROJECT_ADMIN);
    }

    @Test
    void getEffectiveProjectRole_shouldReturnProjectAdminForOrgAdmin() {
        
        orgMember.setRole(OrganizationRole.ORG_ADMIN);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.empty());

        
        ProjectRole result = permissionService.getEffectiveProjectRole(testUser, testProject);

        
        assertThat(result).isEqualTo(ProjectRole.PROJECT_ADMIN);
    }

    @Test
    void getEffectiveProjectRole_shouldRespectExplicitProjectRoleForOrgOwner() {
        
        orgMember.setRole(OrganizationRole.ORG_OWNER);
        projectMember.setRole(ProjectRole.PROJECT_VIEWER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.of(projectMember));

        
        ProjectRole result = permissionService.getEffectiveProjectRole(testUser, testProject);

        
        assertThat(result).isEqualTo(ProjectRole.PROJECT_VIEWER);
    }

    @Test
    void getEffectiveProjectRole_shouldReturnNullForOrgMemberWithoutProjectMembership() {
        
        orgMember.setRole(OrganizationRole.ORG_MEMBER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.empty());

        
        ProjectRole result = permissionService.getEffectiveProjectRole(testUser, testProject);

        
        assertThat(result).isNull();
    }

    @Test
    void getEffectiveProjectRole_shouldReturnProjectRoleForOrgMember() {
        
        orgMember.setRole(OrganizationRole.ORG_MEMBER);
        projectMember.setRole(ProjectRole.PROJECT_MEMBER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.of(projectMember));

        
        ProjectRole result = permissionService.getEffectiveProjectRole(testUser, testProject);

        
        assertThat(result).isEqualTo(ProjectRole.PROJECT_MEMBER);
    }

    @Test
    void canAccessProject_shouldReturnTrueWhenUserHasRole() {
        
        orgMember.setRole(OrganizationRole.ORG_OWNER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.empty());

        
        boolean result = permissionService.canAccessProject(testUser, testProject);

        
        assertThat(result).isTrue();
    }

    @Test
    void canAccessProject_shouldReturnFalseWhenUserHasNoRole() {
        
        orgMember.setRole(OrganizationRole.ORG_MEMBER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.empty());

        
        boolean result = permissionService.canAccessProject(testUser, testProject);

        
        assertThat(result).isFalse();
    }

    @Test
    void canEditProject_shouldReturnTrueForProjectOwner() {
        
        orgMember.setRole(OrganizationRole.ORG_MEMBER);
        projectMember.setRole(ProjectRole.PROJECT_OWNER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.of(projectMember));

        
        boolean result = permissionService.canEditProject(testUser, testProject);

        
        assertThat(result).isTrue();
    }

    @Test
    void canEditProject_shouldReturnTrueForProjectAdmin() {
        
        orgMember.setRole(OrganizationRole.ORG_ADMIN);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.empty());

        
        boolean result = permissionService.canEditProject(testUser, testProject);

        
        assertThat(result).isTrue();
    }

    @Test
    void canEditProject_shouldReturnFalseForProjectMember() {
        
        orgMember.setRole(OrganizationRole.ORG_MEMBER);
        projectMember.setRole(ProjectRole.PROJECT_MEMBER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.of(projectMember));

        
        boolean result = permissionService.canEditProject(testUser, testProject);

        
        assertThat(result).isFalse();
    }

    @Test
    void canDeleteProject_shouldReturnTrueForProjectOwner() {
        
        orgMember.setRole(OrganizationRole.ORG_MEMBER);
        projectMember.setRole(ProjectRole.PROJECT_OWNER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.of(projectMember));

        
        boolean result = permissionService.canDeleteProject(testUser, testProject);

        
        assertThat(result).isTrue();
    }

    @Test
    void canDeleteProject_shouldReturnFalseForProjectAdmin() {
        
        orgMember.setRole(OrganizationRole.ORG_ADMIN);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.empty());

        
        boolean result = permissionService.canDeleteProject(testUser, testProject);

        
        assertThat(result).isFalse();
    }

    @Test
    void canManageMembers_shouldReturnTrueForProjectOwner() {
        
        orgMember.setRole(OrganizationRole.ORG_MEMBER);
        projectMember.setRole(ProjectRole.PROJECT_OWNER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.of(projectMember));

        
        boolean result = permissionService.canManageMembers(testUser, testProject);

        
        assertThat(result).isTrue();
    }

    @Test
    void canManageTasks_shouldReturnTrueForProjectMember() {
        
        orgMember.setRole(OrganizationRole.ORG_MEMBER);
        projectMember.setRole(ProjectRole.PROJECT_MEMBER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.of(projectMember));

        
        boolean result = permissionService.canManageTasks(testUser, testProject);

        
        assertThat(result).isTrue();
    }

    @Test
    void canManageTasks_shouldReturnFalseForProjectViewer() {
        
        orgMember.setRole(OrganizationRole.ORG_MEMBER);
        projectMember.setRole(ProjectRole.PROJECT_VIEWER);
        when(organizationMemberRepository.findByUserIdAndOrganizationId(1, 1L))
                .thenReturn(Optional.of(orgMember));
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L))
                .thenReturn(Optional.of(projectMember));

        
        boolean result = permissionService.canManageTasks(testUser, testProject);

        
        assertThat(result).isFalse();
    }
}
