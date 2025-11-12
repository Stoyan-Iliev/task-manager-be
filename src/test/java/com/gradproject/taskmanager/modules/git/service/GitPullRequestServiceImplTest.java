package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.domain.GitPrTask;
import com.gradproject.taskmanager.modules.git.domain.GitPullRequest;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import com.gradproject.taskmanager.modules.git.domain.enums.LinkMethod;
import com.gradproject.taskmanager.modules.git.domain.enums.PullRequestStatus;
import com.gradproject.taskmanager.modules.git.dto.response.PullRequestResponse;
import com.gradproject.taskmanager.modules.git.repository.GitPrTaskRepository;
import com.gradproject.taskmanager.modules.git.repository.GitPullRequestRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.GitIntegrationMapper;
import com.gradproject.taskmanager.shared.security.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class GitPullRequestServiceImplTest {

    @Mock
    private GitPullRequestRepository gitPullRequestRepository;

    @Mock
    private GitPrTaskRepository gitPrTaskRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GitIntegrationMapper gitIntegrationMapper;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private GitPullRequestServiceImpl gitPullRequestService;

    private User user;
    private Organization organization;
    private Project project;
    private Task task;
    private GitIntegration gitIntegration;
    private GitPullRequest gitPullRequest;
    private PullRequestResponse pullRequestResponse;

    @BeforeEach
    void setUp() {
        
        user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Org");

        
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        project.setOrganization(organization);

        
        task = new Task();
        task.setId(1L);
        task.setKey("PROJ-123");
        task.setTitle("Test Task");
        task.setProject(project);

        
        gitIntegration = new GitIntegration();
        gitIntegration.setId(1L);
        gitIntegration.setProvider(GitProvider.GITHUB);
        gitIntegration.setRepositoryUrl("https://github.com/test/repo");
        gitIntegration.setOrganization(organization);
        gitIntegration.setProject(project);

        
        gitPullRequest = new GitPullRequest();
        gitPullRequest.setId(1L);
        gitPullRequest.setPrNumber(42);
        gitPullRequest.setPrTitle("Test PR");
        gitPullRequest.setPrDescription("Test description");
        gitPullRequest.setStatus(PullRequestStatus.OPEN);
        gitPullRequest.setSourceBranch("feature/test");
        gitPullRequest.setTargetBranch("main");
        gitPullRequest.setGitIntegration(gitIntegration);
        gitPullRequest.setCreatedAt(LocalDateTime.now());

        
        pullRequestResponse = new PullRequestResponse(
            1L, 1L, null, 42, "Test PR", "Test description",
            "https://github.com/test/repo/pull/42",
            PullRequestStatus.OPEN, "feature/test", "main", "abc123",
            "testuser", "Test User", "test@example.com",
            List.of(), 0, 1, false, null, 0, 0, List.of(), false,
            true, false, null, null, null,
            List.of("PROJ-123"), false,
            LocalDateTime.now(), LocalDateTime.now(), null
        );
    }

    @Test
    void getPullRequestsByTask_Success() {
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(gitPullRequestRepository.findByTaskId(1L)).thenReturn(List.of(gitPullRequest));
        when(gitIntegrationMapper.toPullRequestResponse(gitPullRequest)).thenReturn(pullRequestResponse);
        when(gitPrTaskRepository.findByGitPullRequestId(1L)).thenReturn(List.of());

        
        List<PullRequestResponse> result = gitPullRequestService.getPullRequestsByTask(1L, 1);

        
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        verify(taskRepository).findById(1L);
        verify(userRepository).findById(1);
        verify(permissionService).canAccessProject(user, project);
        verify(gitPullRequestRepository).findByTaskId(1L);
    }

    @Test
    void getPullRequestsByTask_TaskNotFound() {
        
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> gitPullRequestService.getPullRequestsByTask(999L, 1))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Task not found");

        verify(taskRepository).findById(999L);
        verifyNoInteractions(gitPullRequestRepository);
    }

    @Test
    void getPullRequestsByTask_UnauthorizedAccess() {
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> gitPullRequestService.getPullRequestsByTask(1L, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("permission");

        verify(permissionService).canAccessProject(user, project);
        verifyNoInteractions(gitPullRequestRepository);
    }

    @Test
    void getPullRequestsByProject_Success() {
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<GitPullRequest> prPage = new PageImpl<>(List.of(gitPullRequest), pageable, 1);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(gitPullRequestRepository.findAll(pageable)).thenReturn(prPage);
        when(gitIntegrationMapper.toPullRequestResponse(gitPullRequest)).thenReturn(pullRequestResponse);
        when(gitPrTaskRepository.findByGitPullRequestId(1L)).thenReturn(List.of());

        
        Page<PullRequestResponse> result = gitPullRequestService.getPullRequestsByProject(1L, 1, pageable);

        
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(projectRepository).findById(1L);
    }

    @Test
    void getPullRequest_Success() {
        
        when(gitPullRequestRepository.findById(1L)).thenReturn(Optional.of(gitPullRequest));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(gitIntegrationMapper.toPullRequestResponse(gitPullRequest)).thenReturn(pullRequestResponse);
        when(gitPrTaskRepository.findByGitPullRequestId(1L)).thenReturn(List.of());

        
        PullRequestResponse result = gitPullRequestService.getPullRequest(1L, 1);

        
        assertThat(result).isNotNull();
        assertThat(result.prNumber()).isEqualTo(42);
        verify(gitPullRequestRepository).findById(1L);
    }

    @Test
    void getPullRequest_NotFound() {
        
        when(gitPullRequestRepository.findById(999L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> gitPullRequestService.getPullRequest(999L, 1))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Pull request not found");

        verify(gitPullRequestRepository).findById(999L);
    }

    @Test
    void getPullRequestByNumber_Success() {
        
        when(gitPullRequestRepository.findByGitIntegrationIdAndPrNumber(1L, 42))
            .thenReturn(Optional.of(gitPullRequest));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(gitIntegrationMapper.toPullRequestResponse(gitPullRequest)).thenReturn(pullRequestResponse);
        when(gitPrTaskRepository.findByGitPullRequestId(1L)).thenReturn(List.of());

        
        PullRequestResponse result = gitPullRequestService.getPullRequestByNumber(1L, 42, 1);

        
        assertThat(result).isNotNull();
        assertThat(result.prNumber()).isEqualTo(42);
    }

    @Test
    void linkPullRequestToTask_Success() {
        
        when(gitPullRequestRepository.findById(1L)).thenReturn(Optional.of(gitPullRequest));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(gitPrTaskRepository.existsByGitPullRequestIdAndTaskId(1L, 1L)).thenReturn(false);
        when(gitPrTaskRepository.save(any(GitPrTask.class))).thenAnswer(i -> i.getArgument(0));
        when(gitIntegrationMapper.toPullRequestResponse(gitPullRequest)).thenReturn(pullRequestResponse);
        when(gitPrTaskRepository.findByGitPullRequestId(1L)).thenReturn(List.of());

        
        PullRequestResponse result = gitPullRequestService.linkPullRequestToTask(1L, 1L, true, 1);

        
        assertThat(result).isNotNull();
        verify(gitPrTaskRepository).save(any(GitPrTask.class));
    }

    @Test
    void linkPullRequestToTask_UnauthorizedAccess() {
        
        when(gitPullRequestRepository.findById(1L)).thenReturn(Optional.of(gitPullRequest));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> gitPullRequestService.linkPullRequestToTask(1L, 1L, true, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("permission");

        verify(permissionService).canManageTasks(user, project);
        verify(gitPrTaskRepository, never()).save(any());
    }

    @Test
    void linkPullRequestToTask_AlreadyLinked() {
        
        when(gitPullRequestRepository.findById(1L)).thenReturn(Optional.of(gitPullRequest));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(gitPrTaskRepository.existsByGitPullRequestIdAndTaskId(1L, 1L)).thenReturn(true);

        
        assertThatThrownBy(() -> gitPullRequestService.linkPullRequestToTask(1L, 1L, false, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already linked");

        verify(gitPrTaskRepository, never()).save(any());
    }

    @Test
    void linkPullRequestToTask_TaskNotInSameProject() {
        
        Project differentProject = new Project();
        differentProject.setId(2L);
        differentProject.setOrganization(organization);

        Task differentTask = new Task();
        differentTask.setId(2L);
        differentTask.setProject(differentProject);

        when(gitPullRequestRepository.findById(1L)).thenReturn(Optional.of(gitPullRequest));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(differentTask));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);

        
        assertThatThrownBy(() -> gitPullRequestService.linkPullRequestToTask(1L, 2L, false, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("same project");

        verify(gitPrTaskRepository, never()).save(any());
    }

    @Test
    void unlinkPullRequestFromTask_Success() {
        
        GitPrTask link = new GitPrTask(gitPullRequest, task, LinkMethod.MANUAL, false);

        when(gitPullRequestRepository.findById(1L)).thenReturn(Optional.of(gitPullRequest));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(gitPrTaskRepository.findByGitPullRequestIdAndTaskId(1L, 1L)).thenReturn(Optional.of(link));
        when(gitIntegrationMapper.toPullRequestResponse(gitPullRequest)).thenReturn(pullRequestResponse);
        when(gitPrTaskRepository.findByGitPullRequestId(1L)).thenReturn(List.of());

        
        PullRequestResponse result = gitPullRequestService.unlinkPullRequestFromTask(1L, 1L, 1);

        
        assertThat(result).isNotNull();
        verify(gitPrTaskRepository).delete(link);
    }

    @Test
    void unlinkPullRequestFromTask_LinkNotFound() {
        
        when(gitPullRequestRepository.findById(1L)).thenReturn(Optional.of(gitPullRequest));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(gitPrTaskRepository.findByGitPullRequestIdAndTaskId(1L, 1L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> gitPullRequestService.unlinkPullRequestFromTask(1L, 1L, 1))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Link not found");

        verify(gitPrTaskRepository, never()).delete(any());
    }

    @Test
    void unlinkPullRequestFromTask_UnauthorizedAccess() {
        
        when(gitPullRequestRepository.findById(1L)).thenReturn(Optional.of(gitPullRequest));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> gitPullRequestService.unlinkPullRequestFromTask(1L, 1L, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("permission");

        verify(permissionService).canManageTasks(user, project);
        verify(gitPrTaskRepository, never()).delete(any());
    }
}
