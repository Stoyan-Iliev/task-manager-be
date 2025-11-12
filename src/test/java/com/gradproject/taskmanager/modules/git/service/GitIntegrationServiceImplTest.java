package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import com.gradproject.taskmanager.modules.git.dto.request.CreateGitIntegrationRequest;
import com.gradproject.taskmanager.modules.git.dto.request.UpdateGitIntegrationRequest;
import com.gradproject.taskmanager.modules.git.dto.response.GitIntegrationResponse;
import com.gradproject.taskmanager.modules.git.repository.GitBranchRepository;
import com.gradproject.taskmanager.modules.git.repository.GitCommitRepository;
import com.gradproject.taskmanager.modules.git.repository.GitIntegrationRepository;
import com.gradproject.taskmanager.modules.git.repository.GitPullRequestRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.shared.exception.DuplicateResourceException;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.GitIntegrationMapper;
import com.gradproject.taskmanager.shared.util.TokenEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class GitIntegrationServiceImplTest {

    @Mock
    private GitIntegrationRepository integrationRepository;

    @Mock
    private GitBranchRepository branchRepository;

    @Mock
    private GitCommitRepository commitRepository;

    @Mock
    private GitPullRequestRepository pullRequestRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GitIntegrationMapper mapper;

    @Mock
    private TokenEncryptionService encryptionService;

    @InjectMocks
    private GitIntegrationServiceImpl integrationService;

    private Organization organization;
    private Project project;
    private User user;
    private GitIntegration integration;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Org");

        project = new Project();
        project.setId(100L);
        project.setKey("PROJ");
        project.setName("Test Project");
        project.setOrganization(organization);

        user = new User();
        user.setId(1);
        user.setUsername("testuser");

        integration = new GitIntegration();
        integration.setId(1L);
        integration.setOrganization(organization);
        integration.setProject(project);
        integration.setProvider(GitProvider.GITHUB);
        integration.setRepositoryUrl("https://github.com/owner/repo");
        integration.setRepositoryOwner("owner");
        integration.setRepositoryName("repo");
        integration.setRepositoryFullName("owner/repo");
        integration.setAccessTokenEncrypted("encrypted_token");
        integration.setIsActive(true);
        integration.setCreatedBy(user);
    }

    @Nested
    class CreateIntegration {

        @Test
        void shouldCreateIntegrationSuccessfully() {
            
            CreateGitIntegrationRequest request = new CreateGitIntegrationRequest(
                100L,
                GitProvider.GITHUB,
                "https://github.com/owner/repo",
                "test_token",
                "webhook_secret",
                true,
                true,
                true,
                "feature/"
            );

            when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(integrationRepository.existsByOrganizationIdAndRepositoryUrl(1L, request.repositoryUrl()))
                .thenReturn(false);
            when(encryptionService.encrypt("test_token")).thenReturn("encrypted_token");
            when(encryptionService.encrypt("webhook_secret")).thenReturn("encrypted_secret");
            when(integrationRepository.save(any(GitIntegration.class))).thenReturn(integration);
            when(mapper.toResponse(integration)).thenReturn(createMockResponse());
            when(branchRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(commitRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(pullRequestRepository.countByGitIntegrationId(1L)).thenReturn(0L);

            
            GitIntegrationResponse response = integrationService.createIntegration(1L, request, 1);

            
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);

            ArgumentCaptor<GitIntegration> captor = ArgumentCaptor.forClass(GitIntegration.class);
            verify(integrationRepository).save(captor.capture());

            GitIntegration savedIntegration = captor.getValue();
            assertThat(savedIntegration.getRepositoryOwner()).isEqualTo("owner");
            assertThat(savedIntegration.getRepositoryName()).isEqualTo("repo");
            assertThat(savedIntegration.getRepositoryFullName()).isEqualTo("owner/repo");
            assertThat(savedIntegration.getAccessTokenEncrypted()).isEqualTo("encrypted_token");
            assertThat(savedIntegration.getWebhookSecretEncrypted()).isEqualTo("encrypted_secret");
        }

        @Test
        void shouldThrowExceptionWhenOrganizationNotFound() {
            
            CreateGitIntegrationRequest request = new CreateGitIntegrationRequest(
                100L, GitProvider.GITHUB, "https://github.com/owner/repo",
                "token", null, true, true, true, null
            );

            when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

            
            assertThatThrownBy(() -> integrationService.createIntegration(1L, request, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organization");
        }

        @Test
        void shouldThrowExceptionWhenProjectNotFound() {
            
            CreateGitIntegrationRequest request = new CreateGitIntegrationRequest(
                999L, GitProvider.GITHUB, "https://github.com/owner/repo",
                "token", null, true, true, true, null
            );

            when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            
            assertThatThrownBy(() -> integrationService.createIntegration(1L, request, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project");
        }

        @Test
        void shouldThrowExceptionWhenProjectDoesNotBelongToOrganization() {
            
            Organization otherOrg = new Organization();
            otherOrg.setId(2L);
            project.setOrganization(otherOrg);

            CreateGitIntegrationRequest request = new CreateGitIntegrationRequest(
                100L, GitProvider.GITHUB, "https://github.com/owner/repo",
                "token", null, true, true, true, null
            );

            when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

            
            assertThatThrownBy(() -> integrationService.createIntegration(1L, request, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("does not belong to this organization");
        }

        @Test
        void shouldThrowExceptionWhenRepositoryAlreadyIntegrated() {
            
            CreateGitIntegrationRequest request = new CreateGitIntegrationRequest(
                100L, GitProvider.GITHUB, "https://github.com/owner/repo",
                "token", null, true, true, true, null
            );

            when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(integrationRepository.existsByOrganizationIdAndRepositoryUrl(1L, request.repositoryUrl()))
                .thenReturn(true);

            
            assertThatThrownBy(() -> integrationService.createIntegration(1L, request, 1))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Git Integration");
        }

        @Test
        void shouldParseGitLabUrlCorrectly() {
            
            CreateGitIntegrationRequest request = new CreateGitIntegrationRequest(
                100L, GitProvider.GITLAB, "https://gitlab.com/group/project",
                "token", null, true, true, true, null
            );

            when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(integrationRepository.existsByOrganizationIdAndRepositoryUrl(anyLong(), anyString()))
                .thenReturn(false);
            when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
            when(integrationRepository.save(any(GitIntegration.class))).thenReturn(integration);
            when(mapper.toResponse(any())).thenReturn(createMockResponse());
            when(branchRepository.countByGitIntegrationId(anyLong())).thenReturn(0L);
            when(commitRepository.countByGitIntegrationId(anyLong())).thenReturn(0L);
            when(pullRequestRepository.countByGitIntegrationId(anyLong())).thenReturn(0L);

            
            integrationService.createIntegration(1L, request, 1);

            
            ArgumentCaptor<GitIntegration> captor = ArgumentCaptor.forClass(GitIntegration.class);
            verify(integrationRepository).save(captor.capture());

            GitIntegration saved = captor.getValue();
            assertThat(saved.getRepositoryOwner()).isEqualTo("group");
            assertThat(saved.getRepositoryName()).isEqualTo("project");
        }

        @Test
        void shouldParseSshUrlCorrectly() {
            
            CreateGitIntegrationRequest request = new CreateGitIntegrationRequest(
                100L, GitProvider.GITHUB, "git@github.com:owner/repo.git",
                "token", null, true, true, true, null
            );

            when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(integrationRepository.existsByOrganizationIdAndRepositoryUrl(anyLong(), anyString()))
                .thenReturn(false);
            when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
            when(integrationRepository.save(any(GitIntegration.class))).thenReturn(integration);
            when(mapper.toResponse(any())).thenReturn(createMockResponse());
            when(branchRepository.countByGitIntegrationId(anyLong())).thenReturn(0L);
            when(commitRepository.countByGitIntegrationId(anyLong())).thenReturn(0L);
            when(pullRequestRepository.countByGitIntegrationId(anyLong())).thenReturn(0L);

            
            integrationService.createIntegration(1L, request, 1);

            
            ArgumentCaptor<GitIntegration> captor = ArgumentCaptor.forClass(GitIntegration.class);
            verify(integrationRepository).save(captor.capture());

            GitIntegration saved = captor.getValue();
            assertThat(saved.getRepositoryOwner()).isEqualTo("owner");
            assertThat(saved.getRepositoryName()).isEqualTo("repo");
        }
    }

    @Nested
    class UpdateIntegration {

        @Test
        void shouldUpdateIntegrationSuccessfully() {
            
            UpdateGitIntegrationRequest request = new UpdateGitIntegrationRequest(
                false, true, false, "bugfix/", true, null, null
            );

            when(integrationRepository.findById(1L)).thenReturn(Optional.of(integration));
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(integrationRepository.save(any(GitIntegration.class))).thenReturn(integration);
            when(mapper.toResponse(integration)).thenReturn(createMockResponse());
            when(branchRepository.countByGitIntegrationId(1L)).thenReturn(5L);
            when(commitRepository.countByGitIntegrationId(1L)).thenReturn(10L);
            when(pullRequestRepository.countByGitIntegrationId(1L)).thenReturn(2L);

            
            GitIntegrationResponse response = integrationService.updateIntegration(1L, request, 1);

            
            assertThat(response).isNotNull();
            verify(integrationRepository).save(integration);
            assertThat(integration.getAutoLinkEnabled()).isFalse();
            assertThat(integration.getSmartCommitsEnabled()).isTrue();
            assertThat(integration.getAutoCloseOnMerge()).isFalse();
            assertThat(integration.getBranchPrefix()).isEqualTo("bugfix/");
            assertThat(integration.getUpdatedBy()).isEqualTo(user);
        }

        @Test
        void shouldUpdateAccessTokenWhenProvided() {
            
            UpdateGitIntegrationRequest request = new UpdateGitIntegrationRequest(
                null, null, null, null, null, "new_token", null
            );

            when(integrationRepository.findById(1L)).thenReturn(Optional.of(integration));
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(encryptionService.encrypt("new_token")).thenReturn("new_encrypted_token");
            when(integrationRepository.save(any(GitIntegration.class))).thenReturn(integration);
            when(mapper.toResponse(integration)).thenReturn(createMockResponse());
            when(branchRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(commitRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(pullRequestRepository.countByGitIntegrationId(1L)).thenReturn(0L);

            
            integrationService.updateIntegration(1L, request, 1);

            
            verify(encryptionService).encrypt("new_token");
            assertThat(integration.getAccessTokenEncrypted()).isEqualTo("new_encrypted_token");
        }

        @Test
        void shouldThrowExceptionWhenIntegrationNotFound() {
            
            UpdateGitIntegrationRequest request = new UpdateGitIntegrationRequest(
                true, true, true, null, null, null, null
            );

            when(integrationRepository.findById(999L)).thenReturn(Optional.empty());

            
            assertThatThrownBy(() -> integrationService.updateIntegration(999L, request, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Git Integration");
        }
    }

    @Nested
    class DeleteIntegration {

        @Test
        void shouldDeleteIntegrationSuccessfully() {
            
            when(integrationRepository.findById(1L)).thenReturn(Optional.of(integration));

            
            integrationService.deleteIntegration(1L, 1);

            
            verify(integrationRepository).delete(integration);
        }

        @Test
        void shouldThrowExceptionWhenIntegrationNotFound() {
            
            when(integrationRepository.findById(999L)).thenReturn(Optional.empty());

            
            assertThatThrownBy(() -> integrationService.deleteIntegration(999L, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Git Integration");
        }
    }

    @Nested
    class GetIntegration {

        @Test
        void shouldGetIntegrationWithStatistics() {
            
            when(integrationRepository.findById(1L)).thenReturn(Optional.of(integration));
            when(mapper.toResponse(integration)).thenReturn(createMockResponse());
            when(branchRepository.countByGitIntegrationId(1L)).thenReturn(5L);
            when(commitRepository.countByGitIntegrationId(1L)).thenReturn(25L);
            when(pullRequestRepository.countByGitIntegrationId(1L)).thenReturn(3L);

            
            GitIntegrationResponse response = integrationService.getIntegration(1L, 1);

            
            assertThat(response).isNotNull();
            verify(branchRepository).countByGitIntegrationId(1L);
            verify(commitRepository).countByGitIntegrationId(1L);
            verify(pullRequestRepository).countByGitIntegrationId(1L);
        }
    }

    @Nested
    class ListIntegrations {

        @Test
        void shouldListOrganizationIntegrations() {
            
            when(integrationRepository.findByOrganizationId(1L))
                .thenReturn(List.of(integration));
            when(mapper.toResponse(integration)).thenReturn(createMockResponse());
            when(branchRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(commitRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(pullRequestRepository.countByGitIntegrationId(1L)).thenReturn(0L);

            
            List<GitIntegrationResponse> responses =
                integrationService.listOrganizationIntegrations(1L, 1);

            
            assertThat(responses).hasSize(1);
            verify(integrationRepository).findByOrganizationId(1L);
        }

        @Test
        void shouldListProjectIntegrations() {
            
            when(integrationRepository.findByProjectId(100L))
                .thenReturn(List.of(integration));
            when(mapper.toResponse(integration)).thenReturn(createMockResponse());
            when(branchRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(commitRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(pullRequestRepository.countByGitIntegrationId(1L)).thenReturn(0L);

            
            List<GitIntegrationResponse> responses =
                integrationService.listProjectIntegrations(100L, 1);

            
            assertThat(responses).hasSize(1);
            verify(integrationRepository).findByProjectId(100L);
        }
    }

    @Nested
    class GetByRepositoryUrl {

        @Test
        void shouldGetIntegrationByRepositoryUrl() {
            
            String repoUrl = "https://github.com/owner/repo";
            when(integrationRepository.findByOrganizationIdAndRepositoryUrl(1L, repoUrl))
                .thenReturn(Optional.of(integration));
            when(mapper.toResponse(integration)).thenReturn(createMockResponse());
            when(branchRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(commitRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(pullRequestRepository.countByGitIntegrationId(1L)).thenReturn(0L);

            
            GitIntegrationResponse response =
                integrationService.getByRepositoryUrl(1L, repoUrl, 1);

            
            assertThat(response).isNotNull();
            verify(integrationRepository).findByOrganizationIdAndRepositoryUrl(1L, repoUrl);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryUrlNotFound() {
            
            String repoUrl = "https://github.com/owner/repo";
            when(integrationRepository.findByOrganizationIdAndRepositoryUrl(1L, repoUrl))
                .thenReturn(Optional.empty());

            
            assertThatThrownBy(() -> integrationService.getByRepositoryUrl(1L, repoUrl, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Git Integration not found");
        }
    }

    @Nested
    class SyncIntegration {

        @Test
        void shouldUpdateLastSyncTime() {
            
            when(integrationRepository.findById(1L)).thenReturn(Optional.of(integration));
            when(integrationRepository.save(integration)).thenReturn(integration);
            when(mapper.toResponse(integration)).thenReturn(createMockResponse());
            when(branchRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(commitRepository.countByGitIntegrationId(1L)).thenReturn(0L);
            when(pullRequestRepository.countByGitIntegrationId(1L)).thenReturn(0L);

            
            GitIntegrationResponse response = integrationService.syncIntegration(1L, 1);

            
            assertThat(response).isNotNull();
            assertThat(integration.getLastSyncAt()).isNotNull();
            verify(integrationRepository).save(integration);
        }
    }

    @Nested
    class TestConnection {

        @Test
        void shouldReturnTrue() {
            
            CreateGitIntegrationRequest request = new CreateGitIntegrationRequest(
                100L, GitProvider.GITHUB, "https://github.com/owner/repo",
                "token", null, true, true, true, null
            );

            
            boolean result = integrationService.testConnection(1L, request, 1);

            
            assertThat(result).isTrue();
        }
    }

    private GitIntegrationResponse createMockResponse() {
        return new GitIntegrationResponse(
            1L, 1L, 100L, GitProvider.GITHUB,
            "https://github.com/owner/repo", "owner", "repo", "owner/repo",
            null, null, null, true, true, true, "feature/",
            true, null, null, "testuser", 0L, 0L, 0L
        );
    }
}
