package com.gradproject.taskmanager.modules.project.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationMemberRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.ProjectMember;
import com.gradproject.taskmanager.modules.project.domain.ProjectRole;
import com.gradproject.taskmanager.modules.project.dto.AddProjectMemberRequest;
import com.gradproject.taskmanager.modules.project.dto.ProjectMemberResponse;
import com.gradproject.taskmanager.modules.project.repository.ProjectMemberRepository;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.shared.exception.BusinessRuleViolationException;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceImplTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private ProjectMapper mapper;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private ProjectMemberServiceImpl projectMemberService;

    private User testUser;
    private User targetUser;
    private Organization testOrganization;
    private Project testProject;
    private ProjectMember testMember;
    private AddProjectMemberRequest addMemberRequest;
    private ProjectMemberResponse memberResponse;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1);

        targetUser = new User("targetuser", "target@example.com", "password");
        targetUser.setId(2);

        testOrganization = new Organization("Test Org", "test-org", "Test Description", testUser);
        testOrganization.setId(1L);

        testProject = new Project();
        testProject.setId(1L);
        testProject.setKey("TEST");
        testProject.setName("Test Project");
        testProject.setOrganization(testOrganization);
        testProject.setCreatedBy(testUser);

        testMember = new ProjectMember();
        testMember.setId(1L);
        testMember.setUser(targetUser);
        testMember.setProject(testProject);
        testMember.setRole(ProjectRole.PROJECT_MEMBER);
        testMember.setAddedAt(LocalDateTime.now());
        testMember.setAddedBy(testUser);

        addMemberRequest = new AddProjectMemberRequest(2, ProjectRole.PROJECT_MEMBER);

        memberResponse = new ProjectMemberResponse(
                1L,
                2,
                "targetuser",
                "target@example.com",
                ProjectRole.PROJECT_MEMBER,
                LocalDateTime.now(),
                "testuser"
        );
    }

    @Test
    void addMember_shouldSucceed() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageMembers(testUser, testProject)).thenReturn(true);
        when(userRepository.findById(2)).thenReturn(Optional.of(targetUser));
        when(projectMemberRepository.existsByUserIdAndProjectId(2, 1L)).thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(testMember);
        when(mapper.toMemberResponse(testMember)).thenReturn(memberResponse);

        
        ProjectMemberResponse result = projectMemberService.addMember(1L, addMemberRequest, 1);

        
        assertThat(result).isNotNull();
        verify(projectMemberRepository).save(argThat(member ->
                member.getUser().equals(targetUser) &&
                member.getProject().equals(testProject) &&
                member.getRole() == ProjectRole.PROJECT_MEMBER
        ));
    }

    @Test
    void addMember_shouldThrowWhenProjectNotFound() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> projectMemberService.addMember(1L, addMemberRequest, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project");
    }

    @Test
    void addMember_shouldThrowWhenUnauthorized() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(targetUser));
        when(permissionService.canManageMembers(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> projectMemberService.addMember(1L, addMemberRequest, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permission");
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    @Test
    void addMember_shouldThrowWhenAlreadyMember() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageMembers(testUser, testProject)).thenReturn(true);
        when(userRepository.findById(2)).thenReturn(Optional.of(targetUser));
        when(projectMemberRepository.existsByUserIdAndProjectId(2, 1L)).thenReturn(true);

        
        assertThatThrownBy(() -> projectMemberService.addMember(1L, addMemberRequest, 1))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Project member");
    }

    @Test
    void listProjectMembers_shouldReturnMembers() {
        
        ProjectMember member2 = new ProjectMember();
        member2.setId(2L);
        member2.setUser(testUser);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);
        when(projectMemberRepository.findByProjectId(1L)).thenReturn(List.of(testMember, member2));
        when(mapper.toMemberResponse(any(ProjectMember.class))).thenReturn(memberResponse);

        
        List<ProjectMemberResponse> result = projectMemberService.listProjectMembers(1L, 1);

        
        assertThat(result).hasSize(2);
        verify(mapper, times(2)).toMemberResponse(any(ProjectMember.class));
    }

    @Test
    void updateMemberRole_shouldSucceed() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageMembers(testUser, testProject)).thenReturn(true);
        when(projectMemberRepository.findByUserIdAndProjectId(2, 1L))
                .thenReturn(Optional.of(testMember));
        when(projectMemberRepository.save(testMember)).thenReturn(testMember);
        when(mapper.toMemberResponse(testMember)).thenReturn(memberResponse);

        
        ProjectMemberResponse result = projectMemberService.updateMemberRole(
                1L, 2, ProjectRole.PROJECT_ADMIN, 1);

        
        assertThat(result).isNotNull();
        verify(projectMemberRepository).save(testMember);
    }

    @Test
    void updateMemberRole_shouldThrowWhenChangingLastOwnerRole() {
        
        ProjectMember ownMember = new ProjectMember();
        ownMember.setUser(testUser);
        ownMember.setProject(testProject);
        ownMember.setRole(ProjectRole.PROJECT_OWNER);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageMembers(testUser, testProject)).thenReturn(true);
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L)).thenReturn(Optional.of(ownMember));
        when(projectMemberRepository.countByProjectIdAndRole(1L, ProjectRole.PROJECT_OWNER)).thenReturn(1L);

        
        assertThatThrownBy(() -> projectMemberService.updateMemberRole(
                1L, 1, ProjectRole.PROJECT_MEMBER, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot change the role of the last project owner");
    }

    @Test
    void updateMemberRole_shouldThrowWhenUnauthorized() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageMembers(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> projectMemberService.updateMemberRole(
                1L, 2, ProjectRole.PROJECT_ADMIN, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void removeMember_shouldSucceed() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageMembers(testUser, testProject)).thenReturn(true);
        when(projectMemberRepository.findByUserIdAndProjectId(2, 1L))
                .thenReturn(Optional.of(testMember));

        
        projectMemberService.removeMember(1L, 2, 1);

        
        verify(projectMemberRepository).delete(testMember);
    }

    @Test
    void removeMember_shouldThrowWhenRemovingLastOwner() {
        
        testMember.setRole(ProjectRole.PROJECT_OWNER);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageMembers(testUser, testProject)).thenReturn(true);
        when(projectMemberRepository.findByUserIdAndProjectId(2, 1L))
                .thenReturn(Optional.of(testMember));
        when(projectMemberRepository.countByProjectIdAndRole(1L, ProjectRole.PROJECT_OWNER))
                .thenReturn(1L);

        
        assertThatThrownBy(() -> projectMemberService.removeMember(1L, 2, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot remove the last project owner");
    }

    @Test
    void removeMember_shouldThrowWhenRemovingLastOwner_self() {
        
        ProjectMember ownMember = new ProjectMember();
        ownMember.setUser(testUser);
        ownMember.setProject(testProject);
        ownMember.setRole(ProjectRole.PROJECT_OWNER);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageMembers(testUser, testProject)).thenReturn(true);
        when(projectMemberRepository.findByUserIdAndProjectId(1, 1L)).thenReturn(Optional.of(ownMember));
        when(projectMemberRepository.countByProjectIdAndRole(1L, ProjectRole.PROJECT_OWNER)).thenReturn(1L);

        
        assertThatThrownBy(() -> projectMemberService.removeMember(1L, 1, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot remove the last project owner");
    }

    @Test
    void removeMember_shouldThrowWhenUnauthorized() {
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(permissionService.canManageMembers(testUser, testProject)).thenReturn(false);

        
        assertThatThrownBy(() -> projectMemberService.removeMember(1L, 2, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permission");
    }
}
