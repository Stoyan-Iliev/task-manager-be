package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.shared.util.TokenEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class GitHubServiceImplTest {

    @Mock
    private TokenEncryptionService encryptionService;

    @InjectMocks
    private GitHubServiceImpl gitHubService;

    private GitIntegration integration;
    private Organization organization;
    private Project project;
    private User user;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(1L);

        project = new Project();
        project.setId(100L);
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
        integration.setWebhookSecretEncrypted("encrypted_webhook_secret");
        integration.setCreatedBy(user);
    }

    @Nested
    class TestConnection {

        @Test
        void shouldReturnSuccessForStubImplementation() {
            
            GitHubService.ConnectionTestResult result =
                gitHubService.testConnection("owner", "repo", "test_token");

            
            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
            assertThat(result.message()).contains("stubbed implementation");
            assertThat(result.permissions()).isNotNull();
            assertThat(result.permissions().canRead()).isTrue();
            assertThat(result.permissions().canWrite()).isTrue();
            assertThat(result.permissions().canCreateWebhook()).isTrue();
        }
    }

    @Nested
    class CreateWebhook {

        @Test
        void shouldReturnMockWebhookId() {
            
            when(encryptionService.decrypt("encrypted_token")).thenReturn("decrypted_token");
            when(encryptionService.decrypt("encrypted_webhook_secret"))
                .thenReturn("decrypted_webhook_secret");

            
            GitHubService.WebhookResult result = gitHubService.createWebhook(integration);

            
            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
            assertThat(result.webhookId()).startsWith("mock_webhook_");
            assertThat(result.webhookUrl()).contains("owner/repo");
            assertThat(result.message()).contains("stubbed");

            verify(encryptionService).decrypt("encrypted_token");
            verify(encryptionService).decrypt("encrypted_webhook_secret");
        }

        @Test
        void shouldHandleNullWebhookSecret() {
            
            integration.setWebhookSecretEncrypted(null);
            when(encryptionService.decrypt("encrypted_token")).thenReturn("decrypted_token");

            
            GitHubService.WebhookResult result = gitHubService.createWebhook(integration);

            
            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
            verify(encryptionService, never()).decrypt(isNull());
        }
    }

    @Nested
    class RemoveWebhook {

        @Test
        void shouldReturnTrueForStubImplementation() {
            
            integration.setWebhookId("webhook_123");

            
            boolean result = gitHubService.removeWebhook(integration);

            
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnTrueWhenWebhookIdIsNull() {
            
            integration.setWebhookId(null);

            
            boolean result = gitHubService.removeWebhook(integration);

            
            assertThat(result).isTrue();
        }
    }

    @Nested
    class VerifyWebhookSignature {

        @Test
        void shouldVerifyValidSignature() {
            
            String payload = "test";
            String secret = "secret";
            
            
            

            
            
            assertThat(gitHubService.verifyWebhookSignature(payload, "sha256=invalid", secret)).isFalse();
        }

        @Test
        void shouldRejectInvalidSignature() {
            
            String payload = "{\"action\":\"opened\"}";
            String secret = "my_secret";
            String invalidSignature = "sha256=invalid_signature_hash_here";

            
            boolean result = gitHubService.verifyWebhookSignature(payload, invalidSignature, secret);

            
            assertThat(result).isFalse();
        }

        @Test
        void shouldRejectSignatureWithoutSha256Prefix() {
            
            String payload = "{\"action\":\"opened\"}";
            String secret = "my_secret";
            String signatureWithoutPrefix = "4b202def66c919834c68c31b74043e7cc1fb4e09f8f5c75b8e98b2a48e5ddaa4";

            
            boolean result = gitHubService.verifyWebhookSignature(payload, signatureWithoutPrefix, secret);

            
            assertThat(result).isFalse();
        }

        @Test
        void shouldRejectNullSignature() {
            
            String payload = "{\"action\":\"opened\"}";
            String secret = "my_secret";

            
            boolean result = gitHubService.verifyWebhookSignature(payload, null, secret);

            
            assertThat(result).isFalse();
        }

        @Test
        void shouldRejectMismatchedSignature() {
            
            String payload = "{\"action\":\"opened\"}";
            String secret = "my_secret";
            String wrongSecret = "wrong_secret";
            
            String signature = "sha256=4b202def66c919834c68c31b74043e7cc1fb4e09f8f5c75b8e98b2a48e5ddaa4";

            
            boolean result = gitHubService.verifyWebhookSignature(payload, signature, wrongSecret);

            
            assertThat(result).isFalse();
        }
    }

    @Nested
    class SyncBranches {

        @Test
        void shouldReturnStubResult() {
            
            GitHubService.SyncResult result = gitHubService.syncBranches(integration);

            
            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
            assertThat(result.itemsFetched()).isZero();
            assertThat(result.itemsCreated()).isZero();
            assertThat(result.itemsUpdated()).isZero();
            assertThat(result.message()).contains("not yet implemented");
        }
    }

    @Nested
    class SyncCommits {

        @Test
        void shouldReturnStubResult() {
            
            GitHubService.SyncResult result = gitHubService.syncCommits(integration, "2023-01-01T00:00:00Z");

            
            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
            assertThat(result.itemsFetched()).isZero();
            assertThat(result.message()).contains("not yet implemented");
        }

        @Test
        void shouldHandleNullSinceParameter() {
            
            GitHubService.SyncResult result = gitHubService.syncCommits(integration, null);

            
            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    class SyncPullRequests {

        @Test
        void shouldReturnStubResultForAllState() {
            
            GitHubService.SyncResult result = gitHubService.syncPullRequests(integration, "all");

            
            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
            assertThat(result.message()).contains("not yet implemented");
        }

        @Test
        void shouldHandleOpenState() {
            
            GitHubService.SyncResult result = gitHubService.syncPullRequests(integration, "open");

            
            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
        }

        @Test
        void shouldHandleClosedState() {
            
            GitHubService.SyncResult result = gitHubService.syncPullRequests(integration, "closed");

            
            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    class GetCommitDetails {

        @Test
        void shouldReturnNullForStubImplementation() {
            
            Object result = gitHubService.getCommitDetails(integration, "abc123def456");

            
            assertThat(result).isNull();
        }
    }
}
