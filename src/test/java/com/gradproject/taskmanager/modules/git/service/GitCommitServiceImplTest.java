package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.git.domain.GitCommit;
import com.gradproject.taskmanager.modules.git.domain.GitCommitTask;
import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import com.gradproject.taskmanager.modules.git.domain.enums.LinkMethod;
import com.gradproject.taskmanager.modules.git.dto.response.CommitResponse;
import com.gradproject.taskmanager.modules.git.repository.GitCommitRepository;
import com.gradproject.taskmanager.modules.git.repository.GitCommitTaskRepository;
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
class GitCommitServiceImplTest {

    @Mock
    private GitCommitRepository gitCommitRepository;

    @Mock
    private GitCommitTaskRepository gitCommitTaskRepository;

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
    private GitCommitServiceImpl gitCommitService;

    private User user;
    private Organization organization;
    private Project project;
    private Task task;
    private GitIntegration gitIntegration;
    private GitCommit gitCommit;
    private CommitResponse commitResponse;

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

        
        gitCommit = new GitCommit();
        gitCommit.setId(1L);
        gitCommit.setCommitSha("abc123def456");
        gitCommit.setMessage("Test commit");
        gitCommit.setAuthorName("Test Author");
        gitCommit.setAuthorEmail("author@example.com");
        gitCommit.setAuthorDate(LocalDateTime.now());
        gitCommit.setGitIntegration(gitIntegration);

        
        commitResponse = new CommitResponse(
            1L, 1L, "abc123def456", "abc123d", null, null,
            "Test Author", "author@example.com", LocalDateTime.now(),
            "Test Committer", "committer@example.com", LocalDateTime.now(),
            "Test commit", null, 10, 5, 2,
            "https://github.com/test/repo/commit/abc123def456",
            List.of("PROJ-123"), List.of(), LocalDateTime.now()
        );
    }

    @Test
    void getCommitsByTask_Success() {
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(gitCommitRepository.findByTaskId(1L)).thenReturn(List.of(gitCommit));
        when(gitIntegrationMapper.toCommitResponse(gitCommit)).thenReturn(commitResponse);
        when(gitCommitTaskRepository.findByGitCommitId(1L)).thenReturn(List.of());

        
        List<CommitResponse> result = gitCommitService.getCommitsByTask(1L, 1);

        
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        verify(taskRepository).findById(1L);
        verify(userRepository).findById(1);
        verify(permissionService).canAccessProject(user, project);
        verify(gitCommitRepository).findByTaskId(1L);
    }

    @Test
    void getCommitsByTask_TaskNotFound() {
        
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> gitCommitService.getCommitsByTask(999L, 1))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Task not found");

        verify(taskRepository).findById(999L);
        verifyNoInteractions(gitCommitRepository);
    }

    @Test
    void getCommitsByTask_UserNotFound() {
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> gitCommitService.getCommitsByTask(1L, 999))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");

        verify(taskRepository).findById(1L);
        verify(userRepository).findById(999);
        verifyNoInteractions(gitCommitRepository);
    }

    @Test
    void getCommitsByTask_UnauthorizedAccess() {
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> gitCommitService.getCommitsByTask(1L, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("permission");

        verify(permissionService).canAccessProject(user, project);
        verifyNoInteractions(gitCommitRepository);
    }

    @Test
    void getCommitsByTaskPaginated_Success() {
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<GitCommit> commitPage = new PageImpl<>(List.of(gitCommit), pageable, 1);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(gitCommitRepository.findByTaskId(1L, pageable)).thenReturn(commitPage);
        when(gitIntegrationMapper.toCommitResponse(gitCommit)).thenReturn(commitResponse);
        when(gitCommitTaskRepository.findByGitCommitId(1L)).thenReturn(List.of());

        
        Page<CommitResponse> result = gitCommitService.getCommitsByTask(1L, 1, pageable);

        
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(gitCommitRepository).findByTaskId(1L, pageable);
    }

    @Test
    void getCommit_Success() {
        
        when(gitCommitRepository.findById(1L)).thenReturn(Optional.of(gitCommit));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(gitIntegrationMapper.toCommitResponse(gitCommit)).thenReturn(commitResponse);
        when(gitCommitTaskRepository.findByGitCommitId(1L)).thenReturn(List.of());

        
        CommitResponse result = gitCommitService.getCommit(1L, 1);

        
        assertThat(result).isNotNull();
        assertThat(result.commitSha()).isEqualTo("abc123def456");
        verify(gitCommitRepository).findById(1L);
    }

    @Test
    void getCommit_NotFound() {
        
        when(gitCommitRepository.findById(999L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> gitCommitService.getCommit(999L, 1))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Commit not found");

        verify(gitCommitRepository).findById(999L);
    }

    @Test
    void getCommitBySha_Success() {
        
        when(gitCommitRepository.findByGitIntegrationIdAndCommitSha(1L, "abc123def456"))
            .thenReturn(Optional.of(gitCommit));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(gitIntegrationMapper.toCommitResponse(gitCommit)).thenReturn(commitResponse);
        when(gitCommitTaskRepository.findByGitCommitId(1L)).thenReturn(List.of());

        
        CommitResponse result = gitCommitService.getCommitBySha(1L, "abc123def456", 1);

        
        assertThat(result).isNotNull();
        assertThat(result.commitSha()).isEqualTo("abc123def456");
    }

    @Test
    void linkCommitToTask_Success() {
        
        when(gitCommitRepository.findById(1L)).thenReturn(Optional.of(gitCommit));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(gitCommitTaskRepository.existsByGitCommitIdAndTaskId(1L, 1L)).thenReturn(false);
        when(gitCommitTaskRepository.save(any(GitCommitTask.class))).thenAnswer(i -> i.getArgument(0));
        when(gitIntegrationMapper.toCommitResponse(gitCommit)).thenReturn(commitResponse);
        when(gitCommitTaskRepository.findByGitCommitId(1L)).thenReturn(List.of());

        
        CommitResponse result = gitCommitService.linkCommitToTask(1L, 1L, 1);

        
        assertThat(result).isNotNull();
        verify(gitCommitTaskRepository).save(any(GitCommitTask.class));
    }

    @Test
    void linkCommitToTask_UnauthorizedAccess() {
        
        when(gitCommitRepository.findById(1L)).thenReturn(Optional.of(gitCommit));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> gitCommitService.linkCommitToTask(1L, 1L, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("permission");

        verify(permissionService).canManageTasks(user, project);
        verify(gitCommitTaskRepository, never()).save(any());
    }

    @Test
    void linkCommitToTask_AlreadyLinked() {
        
        when(gitCommitRepository.findById(1L)).thenReturn(Optional.of(gitCommit));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(gitCommitTaskRepository.existsByGitCommitIdAndTaskId(1L, 1L)).thenReturn(true);

        
        assertThatThrownBy(() -> gitCommitService.linkCommitToTask(1L, 1L, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already linked");

        verify(gitCommitTaskRepository, never()).save(any());
    }

    @Test
    void linkCommitToTask_TaskNotInSameProject() {
        
        Project differentProject = new Project();
        differentProject.setId(2L);
        differentProject.setOrganization(organization);

        Task differentTask = new Task();
        differentTask.setId(2L);
        differentTask.setProject(differentProject);

        when(gitCommitRepository.findById(1L)).thenReturn(Optional.of(gitCommit));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(differentTask));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);

        
        assertThatThrownBy(() -> gitCommitService.linkCommitToTask(1L, 2L, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("same project");

        verify(gitCommitTaskRepository, never()).save(any());
    }

    @Test
    void unlinkCommitFromTask_Success() {
        
        GitCommitTask link = new GitCommitTask(gitCommit, task, LinkMethod.MANUAL);

        when(gitCommitRepository.findById(1L)).thenReturn(Optional.of(gitCommit));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(gitCommitTaskRepository.findByGitCommitIdAndTaskId(1L, 1L)).thenReturn(Optional.of(link));
        when(gitIntegrationMapper.toCommitResponse(gitCommit)).thenReturn(commitResponse);
        when(gitCommitTaskRepository.findByGitCommitId(1L)).thenReturn(List.of());

        
        CommitResponse result = gitCommitService.unlinkCommitFromTask(1L, 1L, 1);

        
        assertThat(result).isNotNull();
        verify(gitCommitTaskRepository).delete(link);
    }

    @Test
    void unlinkCommitFromTask_LinkNotFound() {
        
        when(gitCommitRepository.findById(1L)).thenReturn(Optional.of(gitCommit));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(true);
        when(gitCommitTaskRepository.findByGitCommitIdAndTaskId(1L, 1L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> gitCommitService.unlinkCommitFromTask(1L, 1L, 1))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Link not found");

        verify(gitCommitTaskRepository, never()).delete(any());
    }

    @Test
    void unlinkCommitFromTask_UnauthorizedAccess() {
        
        when(gitCommitRepository.findById(1L)).thenReturn(Optional.of(gitCommit));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canManageTasks(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> gitCommitService.unlinkCommitFromTask(1L, 1L, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("permission");

        verify(permissionService).canManageTasks(user, project);
        verify(gitCommitTaskRepository, never()).delete(any());
    }
}
