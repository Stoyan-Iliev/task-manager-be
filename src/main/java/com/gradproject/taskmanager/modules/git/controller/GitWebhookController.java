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
@RequestMapping("/api/secure/git/webhook")
@RequiredArgsConstructor
public class GitWebhookController {

    private final GitWebhookService gitWebhookService;

    
    @PostMapping("/github")
    public ResponseEntity<Map<String, String>> handleGitHubWebhook(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody Map<String, Object> payload) {

        log.info("Received GitHub webhook: event={}, signature={}", eventType, signature != null ? "present" : "missing");

        try {
            gitWebhookService.processGitHubWebhook(eventType, signature, payload);
            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (Exception e) {
            log.error("Error processing GitHub webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process webhook"));
        }
    }

    
    @PostMapping("/gitlab")
    public ResponseEntity<Map<String, String>> handleGitLabWebhook(
            @RequestHeader("X-Gitlab-Event") String eventType,
            @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
            @RequestBody Map<String, Object> payload) {

        log.info("Received GitLab webhook: event={}, token={}", eventType, token != null ? "present" : "missing");

        try {
            gitWebhookService.processGitLabWebhook(eventType, token, payload);
            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (Exception e) {
            log.error("Error processing GitLab webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process webhook"));
        }
    }

    
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<Page<GitWebhookEvent>>> getWebhookEvents(
            @RequestParam Long integrationId,
            @PageableDefault(size = 20, sort = "receivedAt") Pageable pageable) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Page<GitWebhookEvent> events = gitWebhookService.getWebhookEvents(integrationId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    
    @GetMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<GitWebhookEvent>> getWebhookEvent(@PathVariable Long eventId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        GitWebhookEvent event = gitWebhookService.getWebhookEvent(eventId, userId);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    
    @PostMapping("/events/{eventId}/retry")
    public ResponseEntity<ApiResponse<GitWebhookEvent>> retryWebhookEvent(@PathVariable Long eventId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        GitWebhookEvent event = gitWebhookService.retryWebhookEvent(eventId, userId);
        return ResponseEntity.ok(ApiResponse.success(event));
    }
}
