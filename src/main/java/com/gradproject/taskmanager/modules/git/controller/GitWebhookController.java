package com.gradproject.taskmanager.modules.git.controller;

import com.gradproject.taskmanager.modules.git.domain.GitWebhookEvent;
import com.gradproject.taskmanager.modules.git.service.GitWebhookService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Slf4j
@RestController
@RequiredArgsConstructor
public class GitWebhookController {

    private final GitWebhookService gitWebhookService;

    // Public webhook endpoints for GitHub/GitLab to call without authentication
    @PostMapping("/api/public/webhooks/github")
    public ResponseEntity<Map<String, String>> handleGitHubWebhook(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody String payloadString) {

        log.info("Received GitHub webhook: event={}, delivery={}, signature={}",
                eventType, deliveryId, signature != null ? "present" : "missing");

        try {
            gitWebhookService.processGitHubWebhook(eventType, signature, payloadString);
            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (Exception e) {
            log.error("Error processing GitHub webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process webhook"));
        }
    }

    // Public webhook endpoints for GitHub/GitLab to call without authentication
    @PostMapping("/api/public/webhooks/gitlab")
    public ResponseEntity<Map<String, String>> handleGitLabWebhook(
            @RequestHeader("X-Gitlab-Event") String eventType,
            @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
            @RequestBody String payloadString) {

        log.info("Received GitLab webhook: event={}, token={}", eventType, token != null ? "present" : "missing");

        try {
            gitWebhookService.processGitLabWebhook(eventType, token, payloadString);
            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (Exception e) {
            log.error("Error processing GitLab webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process webhook"));
        }
    }

    // Secured webhook management endpoints
    @GetMapping("/api/secure/git/webhook/events")
    public ResponseEntity<ApiResponse<Page<GitWebhookEvent>>> getWebhookEvents(
            @RequestParam Long integrationId,
            @PageableDefault(size = 20, sort = "receivedAt") Pageable pageable) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Page<GitWebhookEvent> events = gitWebhookService.getWebhookEvents(integrationId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/api/secure/git/webhook/events/{eventId}")
    public ResponseEntity<ApiResponse<GitWebhookEvent>> getWebhookEvent(@PathVariable Long eventId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        GitWebhookEvent event = gitWebhookService.getWebhookEvent(eventId, userId);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @PostMapping("/api/secure/git/webhook/events/{eventId}/retry")
    public ResponseEntity<ApiResponse<GitWebhookEvent>> retryWebhookEvent(@PathVariable Long eventId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        GitWebhookEvent event = gitWebhookService.retryWebhookEvent(eventId, userId);
        return ResponseEntity.ok(ApiResponse.success(event));
    }
}
