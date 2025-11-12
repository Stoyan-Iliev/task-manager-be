package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.GitWebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;


public interface GitWebhookService {

    
    GitWebhookEvent processGitHubWebhook(String eventType, String signature, Map<String, Object> payload);

    
    GitWebhookEvent processGitLabWebhook(String eventType, String token, Map<String, Object> payload);

    
    Page<GitWebhookEvent> getWebhookEvents(Long integrationId, Integer userId, Pageable pageable);

    
    GitWebhookEvent getWebhookEvent(Long eventId, Integer userId);

    
    GitWebhookEvent retryWebhookEvent(Long eventId, Integer userId);

    
    boolean validateGitHubSignature(String payload, String signature, String secret);

    
    boolean validateGitLabToken(String token, String expectedToken);
}
