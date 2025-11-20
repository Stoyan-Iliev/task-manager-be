package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.GitWebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface GitWebhookService {

    // Process webhook with raw payload string for signature validation
    GitWebhookEvent processGitHubWebhook(String eventType, String signature, String payloadString);

    // Process webhook with raw payload string for token validation
    GitWebhookEvent processGitLabWebhook(String eventType, String token, String payloadString);

    
    Page<GitWebhookEvent> getWebhookEvents(Long integrationId, Integer userId, Pageable pageable);

    
    GitWebhookEvent getWebhookEvent(Long eventId, Integer userId);

    
    GitWebhookEvent retryWebhookEvent(Long eventId, Integer userId);

    
    boolean validateGitHubSignature(String payload, String signature, String secret);

    
    boolean validateGitLabToken(String token, String expectedToken);
}
