package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.domain.GitWebhookEvent;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import com.gradproject.taskmanager.modules.git.repository.GitIntegrationRepository;
import com.gradproject.taskmanager.modules.git.repository.GitWebhookEventRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class GitWebhookServiceImplTest {

    @Mock
    private GitWebhookEventRepository webhookEventRepository;

    @Mock
    private GitIntegrationRepository gitIntegrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private GitWebhookServiceImpl gitWebhookService;

    private User user;
    private Organization organization;
    private Project project;
    private GitIntegration gitIntegration;
    private GitWebhookEvent webhookEvent;
    private Map<String, Object> payload;

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

        
        gitIntegration = new GitIntegration();
        gitIntegration.setId(1L);
        gitIntegration.setProvider(GitProvider.GITHUB);
        gitIntegration.setRepositoryUrl("https://github.com/test/repo");
        gitIntegration.setOrganization(organization);
        gitIntegration.setProject(project);
        gitIntegration.setWebhookSecretEncrypted("test-secret");

        
        webhookEvent = new GitWebhookEvent();
        webhookEvent.setId(1L);
        webhookEvent.setProvider(GitProvider.GITHUB);
        webhookEvent.setEventType("push");
        webhookEvent.setGitIntegration(gitIntegration);
        webhookEvent.setProcessed(false);
        webhookEvent.setRetryCount(0);
        webhookEvent.setReceivedAt(LocalDateTime.now());

        
        payload = new HashMap<>();
        Map<String, Object> repository = new HashMap<>();
        repository.put("html_url", "https://github.com/test/repo");
        payload.put("repository", repository);
    }

    @Test
    void processGitHubWebhook_Success() {
        
        when(gitIntegrationRepository.findByRepositoryUrl("https://github.com/test/repo"))
            .thenReturn(Optional.of(gitIntegration));
        when(webhookEventRepository.save(any(GitWebhookEvent.class)))
            .thenAnswer(i -> i.getArgument(0));

        
        GitWebhookEvent result = gitWebhookService.processGitHubWebhook("push", "sha256=abc123", payload);

        
        assertThat(result).isNotNull();
        assertThat(result.getProvider()).isEqualTo(GitProvider.GITHUB);
        assertThat(result.getEventType()).isEqualTo("push");
        verify(webhookEventRepository).save(any(GitWebhookEvent.class));
    }

    @Test
    void processGitHubWebhook_NoIntegrationFound() {
        
        when(gitIntegrationRepository.findByRepositoryUrl("https://github.com/test/repo"))
            .thenReturn(Optional.empty());
        when(webhookEventRepository.save(any(GitWebhookEvent.class)))
            .thenAnswer(i -> i.getArgument(0));

        
        GitWebhookEvent result = gitWebhookService.processGitHubWebhook("push", "sha256=abc123", payload);

        
        assertThat(result).isNotNull();
        assertThat(result.getGitIntegration()).isNull();
        verify(webhookEventRepository).save(any(GitWebhookEvent.class));
    }

    @Test
    void processGitLabWebhook_Success() {
        
        when(gitIntegrationRepository.findByRepositoryUrl("https://github.com/test/repo"))
            .thenReturn(Optional.of(gitIntegration));
        when(webhookEventRepository.save(any(GitWebhookEvent.class)))
            .thenAnswer(i -> i.getArgument(0));

        
        GitWebhookEvent result = gitWebhookService.processGitLabWebhook("push", "test-secret", payload);

        
        assertThat(result).isNotNull();
        assertThat(result.getProvider()).isEqualTo(GitProvider.GITLAB);
        assertThat(result.getEventType()).isEqualTo("push");
        verify(webhookEventRepository).save(any(GitWebhookEvent.class));
    }

    @Test
    void processGitLabWebhook_InvalidToken() {
        
        when(gitIntegrationRepository.findByRepositoryUrl("https://github.com/test/repo"))
            .thenReturn(Optional.of(gitIntegration));
        when(webhookEventRepository.save(any(GitWebhookEvent.class)))
            .thenAnswer(i -> i.getArgument(0));

        
        GitWebhookEvent result = gitWebhookService.processGitLabWebhook("push", "invalid-token", payload);

        
        assertThat(result).isNotNull();
        assertThat(result.getProcessingError()).isNotNull();
        assertThat(result.getProcessingError()).contains("Invalid webhook token");
        verify(webhookEventRepository).save(any(GitWebhookEvent.class));
    }

    @Test
    void getWebhookEvents_Success() {
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<GitWebhookEvent> eventPage = new PageImpl<>(List.of(webhookEvent), pageable, 1);

        when(gitIntegrationRepository.findById(1L)).thenReturn(Optional.of(gitIntegration));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);
        when(webhookEventRepository.findByGitIntegrationIdOrderByReceivedAtDesc(1L, pageable))
            .thenReturn(eventPage);

        
        Page<GitWebhookEvent> result = gitWebhookService.getWebhookEvents(1L, 1, pageable);

        
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(gitIntegrationRepository).findById(1L);
        verify(permissionService).canAccessProject(user, project);
    }

    @Test
    void getWebhookEvents_IntegrationNotFound() {
        
        Pageable pageable = PageRequest.of(0, 10);
        when(gitIntegrationRepository.findById(999L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> gitWebhookService.getWebhookEvents(999L, 1, pageable))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Git integration not found");

        verify(gitIntegrationRepository).findById(999L);
        verifyNoInteractions(webhookEventRepository);
    }

    @Test
    void getWebhookEvents_UnauthorizedAccess() {
        
        Pageable pageable = PageRequest.of(0, 10);
        when(gitIntegrationRepository.findById(1L)).thenReturn(Optional.of(gitIntegration));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> gitWebhookService.getWebhookEvents(1L, 1, pageable))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("permission");

        verify(permissionService).canAccessProject(user, project);
        verifyNoInteractions(webhookEventRepository);
    }

    @Test
    void getWebhookEvent_Success() {
        
        when(webhookEventRepository.findById(1L)).thenReturn(Optional.of(webhookEvent));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canAccessProject(user, project)).thenReturn(true);

        
        GitWebhookEvent result = gitWebhookService.getWebhookEvent(1L, 1);

        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(webhookEventRepository).findById(1L);
    }

    @Test
    void getWebhookEvent_NotFound() {
        
        when(webhookEventRepository.findById(999L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> gitWebhookService.getWebhookEvent(999L, 1))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Webhook event not found");

        verify(webhookEventRepository).findById(999L);
    }

    @Test
    void getWebhookEvent_WithoutIntegration() {
        
        webhookEvent.setGitIntegration(null);
        when(webhookEventRepository.findById(1L)).thenReturn(Optional.of(webhookEvent));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        
        GitWebhookEvent result = gitWebhookService.getWebhookEvent(1L, 1);

        
        assertThat(result).isNotNull();
        assertThat(result.getGitIntegration()).isNull();
        verify(webhookEventRepository).findById(1L);
        verifyNoInteractions(permissionService);
    }

    @Test
    void retryWebhookEvent_Success() {
        
        webhookEvent.setProcessed(true);
        webhookEvent.setProcessingError("Some error");
        webhookEvent.setRetryCount(1);

        when(webhookEventRepository.findById(1L)).thenReturn(Optional.of(webhookEvent));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canEditProject(user, project)).thenReturn(true);
        when(webhookEventRepository.save(any(GitWebhookEvent.class)))
            .thenAnswer(i -> i.getArgument(0));

        
        GitWebhookEvent result = gitWebhookService.retryWebhookEvent(1L, 1);

        
        assertThat(result).isNotNull();
        assertThat(result.getProcessed()).isFalse();
        assertThat(result.getProcessingError()).isNull();
        assertThat(result.getRetryCount()).isEqualTo(2);
        verify(webhookEventRepository).save(any(GitWebhookEvent.class));
    }

    @Test
    void retryWebhookEvent_UnauthorizedAccess() {
        
        when(webhookEventRepository.findById(1L)).thenReturn(Optional.of(webhookEvent));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(permissionService.canEditProject(user, project)).thenReturn(false);

        
        assertThatThrownBy(() -> gitWebhookService.retryWebhookEvent(1L, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("permission");

        verify(permissionService).canEditProject(user, project);
        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    void retryWebhookEvent_NoIntegration() {
        
        webhookEvent.setGitIntegration(null);
        when(webhookEventRepository.findById(1L)).thenReturn(Optional.of(webhookEvent));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        
        assertThatThrownBy(() -> gitWebhookService.retryWebhookEvent(1L, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("without associated integration");

        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    void validateGitLabToken_Success() {
        
        boolean result = gitWebhookService.validateGitLabToken("test-token", "test-token");

        
        assertThat(result).isTrue();
    }

    @Test
    void validateGitLabToken_Invalid() {
        
        boolean result = gitWebhookService.validateGitLabToken("wrong-token", "test-token");

        
        assertThat(result).isFalse();
    }

    @Test
    void validateGitLabToken_NullExpected() {
        
        boolean result = gitWebhookService.validateGitLabToken("test-token", null);

        
        assertThat(result).isFalse();
    }
}
