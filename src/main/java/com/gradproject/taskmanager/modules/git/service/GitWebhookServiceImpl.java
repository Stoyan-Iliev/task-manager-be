package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.domain.GitWebhookEvent;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import com.gradproject.taskmanager.modules.git.repository.GitIntegrationRepository;
import com.gradproject.taskmanager.modules.git.repository.GitWebhookEventRepository;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class GitWebhookServiceImpl implements GitWebhookService {

    private final GitWebhookEventRepository webhookEventRepository;
    private final GitIntegrationRepository gitIntegrationRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    @Override
    @Transactional
    public GitWebhookEvent processGitHubWebhook(String eventType, String signature, Map<String, Object> payload) {
        log.info("Received GitHub webhook event: {}", eventType);

        
        String repositoryUrl = extractRepositoryUrl(payload);
        Optional<GitIntegration> integration = gitIntegrationRepository.findByRepositoryUrl(repositoryUrl);

        GitWebhookEvent event = new GitWebhookEvent(GitProvider.GITHUB, eventType, payload);
        event.setSignature(signature);

        if (integration.isPresent()) {
            event.setGitIntegration(integration.get());

            
            String webhookSecret = integration.get().getWebhookSecretEncrypted();
            if (webhookSecret != null) {
                
                log.debug("GitHub webhook signature validation not yet implemented");
            }
        } else {
            log.warn("No integration found for repository: {}", repositoryUrl);
        }

        
        GitWebhookEvent saved = webhookEventRepository.save(event);

        
        

        return saved;
    }

    @Override
    @Transactional
    public GitWebhookEvent processGitLabWebhook(String eventType, String token, Map<String, Object> payload) {
        log.info("Received GitLab webhook event: {}", eventType);

        
        String repositoryUrl = extractRepositoryUrl(payload);
        Optional<GitIntegration> integration = gitIntegrationRepository.findByRepositoryUrl(repositoryUrl);

        
        String eventAction = extractEventAction(payload);

        GitWebhookEvent event = new GitWebhookEvent(GitProvider.GITLAB, eventType, payload);
        event.setEventAction(eventAction);
        event.setSignature(token);

        if (integration.isPresent()) {
            event.setGitIntegration(integration.get());

            
            String webhookSecret = integration.get().getWebhookSecretEncrypted();
            if (webhookSecret != null && !webhookSecret.equals(token)) {
                log.warn("Invalid GitLab webhook token for repository: {}", repositoryUrl);
                event.setProcessingError("Invalid webhook token");
                return webhookEventRepository.save(event);
            }
        } else {
            log.warn("No integration found for repository: {}", repositoryUrl);
        }

        
        GitWebhookEvent saved = webhookEventRepository.save(event);

        

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GitWebhookEvent> getWebhookEvents(Long integrationId, Integer userId, Pageable pageable) {
        GitIntegration integration = gitIntegrationRepository.findById(integrationId)
            .orElseThrow(() -> new ResourceNotFoundException("Git integration not found with id: " + integrationId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!permissionService.canAccessProject(user, integration.getProject())) {
            throw new UnauthorizedException("You don't have permission to view this integration's webhook events");
        }

        return webhookEventRepository.findByGitIntegrationIdOrderByReceivedAtDesc(integrationId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public GitWebhookEvent getWebhookEvent(Long eventId, Integer userId) {
        GitWebhookEvent event = webhookEventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Webhook event not found with id: " + eventId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (event.getGitIntegration() != null) {
            if (!permissionService.canAccessProject(user, event.getGitIntegration().getProject())) {
                throw new UnauthorizedException("You don't have permission to view this webhook event");
            }
        }

        return event;
    }

    @Override
    @Transactional
    public GitWebhookEvent retryWebhookEvent(Long eventId, Integer userId) {
        GitWebhookEvent event = webhookEventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Webhook event not found with id: " + eventId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (event.getGitIntegration() != null) {
            if (!permissionService.canEditProject(user, event.getGitIntegration().getProject())) {
                throw new UnauthorizedException("You don't have permission to retry this webhook event");
            }
        } else {
            throw new IllegalArgumentException("Cannot retry webhook event without associated integration");
        }

        
        event.setProcessed(false);
        event.setProcessingStartedAt(null);
        event.setProcessingCompletedAt(null);
        event.setProcessingError(null);
        event.setRetryCount(event.getRetryCount() + 1);

        GitWebhookEvent updated = webhookEventRepository.save(event);

        

        return updated;
    }

    @Override
    public boolean validateGitHubSignature(String payload, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + HexFormat.of().formatHex(hash);

            return expectedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error validating GitHub signature", e);
            return false;
        }
    }

    @Override
    public boolean validateGitLabToken(String token, String expectedToken) {
        return expectedToken != null && expectedToken.equals(token);
    }

    
    private String extractRepositoryUrl(Map<String, Object> payload) {
        
        

        if (payload.containsKey("repository")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> repo = (Map<String, Object>) payload.get("repository");
            if (repo.containsKey("html_url")) {
                return (String) repo.get("html_url");
            }
            if (repo.containsKey("url")) {
                return (String) repo.get("url");
            }
        }

        if (payload.containsKey("project")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> project = (Map<String, Object>) payload.get("project");
            if (project.containsKey("web_url")) {
                return (String) project.get("web_url");
            }
            if (project.containsKey("http_url")) {
                return (String) project.get("http_url");
            }
        }

        return null;
    }

    
    private String extractEventAction(Map<String, Object> payload) {
        if (payload.containsKey("action")) {
            return (String) payload.get("action");
        }
        if (payload.containsKey("object_attributes")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = (Map<String, Object>) payload.get("object_attributes");
            if (attrs.containsKey("action")) {
                return (String) attrs.get("action");
            }
        }
        return null;
    }
}
