package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.shared.util.TokenEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;


@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements GitHubService {

    private final TokenEncryptionService encryptionService;

    
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String WEBHOOK_SIGNATURE_ALGORITHM = "HmacSHA256";

    @Override
    public ConnectionTestResult testConnection(String repositoryOwner, String repositoryName, String accessToken) {
        log.info("Testing GitHub connection for {}/{}", repositoryOwner, repositoryName);

        
        
        
        
        

        
        log.warn("GitHub connection test is stubbed - returning success");

        RepositoryPermissions permissions = new RepositoryPermissions(
            true,   
            true,   
            false,  
            true    
        );

        return new ConnectionTestResult(
            true,
            "Connection successful (stubbed implementation)",
            permissions
        );
    }

    @Override
    public WebhookResult createWebhook(GitIntegration integration) {
        log.info("Creating GitHub webhook for integration {}", integration.getId());

        
        String accessToken = encryptionService.decrypt(integration.getAccessTokenEncrypted());
        String webhookSecret = integration.getWebhookSecretEncrypted() != null ?
            encryptionService.decrypt(integration.getWebhookSecretEncrypted()) : null;

        
        
        
        
        
        
        
        
        
        
        
        

        
        log.warn("Webhook creation is stubbed - returning mock webhook ID");

        String mockWebhookId = "mock_webhook_" + System.currentTimeMillis();
        String mockWebhookUrl = GITHUB_API_BASE + "/repos/" +
            integration.getRepositoryFullName() + "/hooks/" + mockWebhookId;

        return new WebhookResult(
            true,
            mockWebhookId,
            mockWebhookUrl,
            "Webhook created successfully (stubbed)"
        );
    }

    @Override
    public boolean removeWebhook(GitIntegration integration) {
        log.info("Removing GitHub webhook for integration {}", integration.getId());

        if (integration.getWebhookId() == null) {
            log.warn("No webhook ID found for integration {}", integration.getId());
            return true;
        }

        
        

        
        log.warn("Webhook removal is stubbed - returning success");
        return true;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String webhookSecret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            log.warn("Invalid signature format: {}", signature);
            return false;
        }

        try {
            
            String expectedSignature = signature.substring(7);

            
            Mac mac = Mac.getInstance(WEBHOOK_SIGNATURE_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                WEBHOOK_SIGNATURE_ALGORITHM
            );
            mac.init(secretKey);

            byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(signatureBytes);

            boolean isValid = computedSignature.equalsIgnoreCase(expectedSignature);

            if (!isValid) {
                log.warn("Webhook signature mismatch. Expected: {}, Computed: {}",
                    expectedSignature, computedSignature);
            }

            return isValid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    @Override
    public SyncResult syncBranches(GitIntegration integration) {
        log.info("Syncing branches for integration {}", integration.getId());

        
        
        
        

        
        log.warn("Branch sync is stubbed - returning mock result");
        return new SyncResult(
            true,
            0,  
            0,  
            0,  
            "Branch sync not yet implemented"
        );
    }

    @Override
    public SyncResult syncCommits(GitIntegration integration, String since) {
        log.info("Syncing commits for integration {} since {}", integration.getId(), since);

        
        
        
        
        
        
        
        

        
        log.warn("Commit sync is stubbed - returning mock result");
        return new SyncResult(
            true,
            0,  
            0,  
            0,  
            "Commit sync not yet implemented"
        );
    }

    @Override
    public SyncResult syncPullRequests(GitIntegration integration, String state) {
        log.info("Syncing pull requests for integration {} with state {}", integration.getId(), state);

        
        
        
        
        
        
        
        

        
        log.warn("Pull request sync is stubbed - returning mock result");
        return new SyncResult(
            true,
            0,  
            0,  
            0,  
            "Pull request sync not yet implemented"
        );
    }

    @Override
    public Object getCommitDetails(GitIntegration integration, String commitSha) {
        log.info("Fetching commit details for {} in integration {}", commitSha, integration.getId());

        
        
        

        
        log.warn("Commit details fetch is stubbed - returning null");
        return null;
    }
}
