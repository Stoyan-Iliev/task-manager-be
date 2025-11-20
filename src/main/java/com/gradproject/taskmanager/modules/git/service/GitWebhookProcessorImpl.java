package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.*;
import com.gradproject.taskmanager.modules.git.domain.enums.BranchStatus;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import com.gradproject.taskmanager.modules.git.domain.enums.PullRequestStatus;
import com.gradproject.taskmanager.modules.git.repository.GitBranchRepository;
import com.gradproject.taskmanager.modules.git.repository.GitCommitRepository;
import com.gradproject.taskmanager.modules.git.repository.GitPullRequestRepository;
import com.gradproject.taskmanager.modules.git.repository.GitWebhookEventRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class GitWebhookProcessorImpl implements GitWebhookProcessor {

    private final GitWebhookEventRepository webhookEventRepository;
    private final GitCommitRepository commitRepository;
    private final GitPullRequestRepository pullRequestRepository;
    private final GitBranchRepository branchRepository;
    private final GitLinkingService linkingService;
    private final SmartCommitService smartCommitService;

    @Override
    @Async
    @Transactional
    public void processWebhookEvent(GitWebhookEvent event) {
        Long eventId = event.getId();
        log.info("Processing webhook event: id={}, provider={}, eventType={}",
                eventId, event.getProvider(), event.getEventType());

        try {
            // Reload the event from DB to get a managed entity in this transaction
            GitWebhookEvent managedEvent = webhookEventRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalStateException("Webhook event not found: " + eventId));

            managedEvent.setProcessingStartedAt(LocalDateTime.now());
            managedEvent.setProcessed(false);
            webhookEventRepository.save(managedEvent);

            // Dispatch based on provider
            if (managedEvent.getProvider() == GitProvider.GITHUB) {
                processGitHubEvent(managedEvent);
            } else if (managedEvent.getProvider() == GitProvider.GITLAB) {
                processGitLabEvent(managedEvent);
            } else {
                throw new IllegalArgumentException("Unsupported provider: " + managedEvent.getProvider());
            }

            // Mark as successfully processed
            managedEvent.setProcessed(true);
            managedEvent.setProcessingCompletedAt(LocalDateTime.now());
            managedEvent.setProcessingError(null);
            webhookEventRepository.save(managedEvent);

            log.info("Successfully processed webhook event: id={}", eventId);

        } catch (Exception e) {
            log.error("Error processing webhook event: id={}", eventId, e);

            // Reload again in case of error to avoid stale data
            webhookEventRepository.findById(eventId).ifPresent(managedEvent -> {
                managedEvent.setProcessed(false);
                managedEvent.setProcessingError(e.getMessage());
                managedEvent.setProcessingCompletedAt(LocalDateTime.now());
                webhookEventRepository.save(managedEvent);
            });
        }
    }

    @Override
    @Async
    @Transactional
    public void retryWebhookEvent(GitWebhookEvent event) {
        log.info("Retrying webhook event: id={}", event.getId());
        // Simply pass the event - processWebhookEvent will reload it from DB
        processWebhookEvent(event);
    }

    // ==================== GitHub Event Handlers ====================

    private void processGitHubEvent(GitWebhookEvent event) {
        String eventType = event.getEventType();
        Map<String, Object> payload = event.getPayload();

        switch (eventType) {
            case "push" -> handleGitHubPushEvent(event, payload);
            case "pull_request" -> handleGitHubPullRequestEvent(event, payload);
            case "create" -> handleGitHubCreateEvent(event, payload);
            case "delete" -> handleGitHubDeleteEvent(event, payload);
            default -> log.warn("Unsupported GitHub event type: {}", eventType);
        }
    }

    private void handleGitHubPushEvent(GitWebhookEvent event, Map<String, Object> payload) {
        log.debug("Handling GitHub push event");

        GitIntegration integration = event.getGitIntegration();
        if (integration == null || !integration.getAutoLinkEnabled()) {
            log.debug("Auto-linking disabled for integration, skipping");
            return;
        }

        // Extract branch name from ref (e.g., "refs/heads/main" -> "main")
        String ref = (String) payload.get("ref");
        String branchName = ref != null && ref.startsWith("refs/heads/")
                ? ref.substring("refs/heads/".length())
                : ref;

        // Extract commits array
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");

        if (commits == null || commits.isEmpty()) {
            log.debug("No commits in push event, skipping");
            return;
        }

        log.info("Processing {} commits from push event", commits.size());

        for (Map<String, Object> commitData : commits) {
            try {
                processCommitFromPush(integration, commitData, branchName);
            } catch (Exception e) {
                log.error("Error processing commit: {}", commitData.get("id"), e);
            }
        }
    }

    private void processCommitFromPush(GitIntegration integration, Map<String, Object> commitData, String branchName) {
        String commitSha = (String) commitData.get("id");
        String message = (String) commitData.get("message");

        // Check if commit already exists
        Optional<GitCommit> existingCommit = commitRepository
                .findByGitIntegrationIdAndCommitSha(integration.getId(), commitSha);

        if (existingCommit.isPresent()) {
            log.debug("Commit already exists: {}", commitSha);
            return;
        }

        // Create new GitCommit
        GitCommit commit = new GitCommit(integration, commitSha, message);
        commit.setBranchName(branchName);

        // Extract author info
        @SuppressWarnings("unchecked")
        Map<String, Object> author = (Map<String, Object>) commitData.get("author");
        if (author != null) {
            commit.setAuthorName((String) author.get("name"));
            commit.setAuthorEmail((String) author.get("email"));

            String timestamp = (String) author.get("timestamp");
            if (timestamp != null) {
                commit.setAuthorDate(parseIsoTimestamp(timestamp));
            }
        }

        // Extract committer info
        @SuppressWarnings("unchecked")
        Map<String, Object> committer = (Map<String, Object>) commitData.get("committer");
        if (committer != null) {
            commit.setCommitterName((String) committer.get("name"));
            commit.setCommitterEmail((String) committer.get("email"));

            String timestamp = (String) committer.get("timestamp");
            if (timestamp != null) {
                commit.setCommitterDate(parseIsoTimestamp(timestamp));
            }
        }

        // Extract stats
        @SuppressWarnings("unchecked")
        List<String> added = (List<String>) commitData.get("added");
        @SuppressWarnings("unchecked")
        List<String> modified = (List<String>) commitData.get("modified");
        @SuppressWarnings("unchecked")
        List<String> removed = (List<String>) commitData.get("removed");

        int filesChanged = (added != null ? added.size() : 0) +
                (modified != null ? modified.size() : 0) +
                (removed != null ? removed.size() : 0);
        commit.setFilesChanged(filesChanged);

        // Set commit URL
        String commitUrl = (String) commitData.get("url");
        commit.setCommitUrl(commitUrl);

        // Save commit
        commit = commitRepository.save(commit);
        log.info("Created commit: {}", commitSha);

        // Link commit to tasks if auto-linking is enabled
        if (integration.getAutoLinkEnabled()) {
            List<Task> linkedTasks = linkingService.linkCommitToTasks(commit);
            log.info("Linked commit {} to {} tasks", commitSha, linkedTasks.size());

            // Execute smart commits if enabled
            if (integration.getSmartCommitsEnabled() && !linkedTasks.isEmpty()) {
                try {
                    SmartCommitService.SmartCommitExecutionSummary summary =
                            smartCommitService.processCommit(commit);
                    log.info("Smart commit execution: {} successful, {} failed",
                            summary.successfulExecutions(), summary.failedExecutions());
                } catch (Exception e) {
                    log.error("Error executing smart commits for {}", commitSha, e);
                }
            }
        }
    }

    private void handleGitHubPullRequestEvent(GitWebhookEvent event, Map<String, Object> payload) {
        log.debug("Handling GitHub pull_request event");

        GitIntegration integration = event.getGitIntegration();
        if (integration == null) {
            log.debug("No integration found, skipping");
            return;
        }

        String action = (String) payload.get("action");
        @SuppressWarnings("unchecked")
        Map<String, Object> prData = (Map<String, Object>) payload.get("pull_request");

        if (prData == null) {
            log.warn("No pull_request data in payload");
            return;
        }

        Integer prNumber = (Integer) prData.get("number");
        String prTitle = (String) prData.get("title");
        String prDescription = (String) prData.get("body");
        String prUrl = (String) prData.get("html_url");
        String state = (String) prData.get("state");
        Boolean merged = (Boolean) prData.get("merged");

        // Find or create PR
        Optional<GitPullRequest> existingPr = pullRequestRepository
                .findByGitIntegrationIdAndPrNumber(integration.getId(), prNumber);

        GitPullRequest pr;
        if (existingPr.isPresent()) {
            pr = existingPr.get();
            log.debug("Updating existing PR: {}", prNumber);
        } else {
            pr = new GitPullRequest();
            pr.setGitIntegration(integration);
            pr.setPrNumber(prNumber);
            log.debug("Creating new PR: {}", prNumber);
        }

        // Update PR fields
        pr.setPrTitle(prTitle);
        pr.setPrDescription(prDescription);
        pr.setPrUrl(prUrl);

        // Set status
        if (Boolean.TRUE.equals(merged)) {
            pr.setStatus(PullRequestStatus.MERGED);
            pr.setMerged(true);

            String mergedAtStr = (String) prData.get("merged_at");
            if (mergedAtStr != null) {
                pr.setMergedAt(parseIsoTimestamp(mergedAtStr));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> mergedBy = (Map<String, Object>) prData.get("merged_by");
            if (mergedBy != null) {
                pr.setMergedBy((String) mergedBy.get("login"));
            }

            String mergeCommitSha = (String) prData.get("merge_commit_sha");
            pr.setMergeCommitSha(mergeCommitSha);
        } else if ("closed".equals(state)) {
            pr.setStatus(PullRequestStatus.CLOSED);

            String closedAtStr = (String) prData.get("closed_at");
            if (closedAtStr != null) {
                pr.setClosedAt(parseIsoTimestamp(closedAtStr));
            }
        } else if ("open".equals(state)) {
            Boolean draft = (Boolean) prData.get("draft");
            pr.setStatus(Boolean.TRUE.equals(draft) ? PullRequestStatus.DRAFT : PullRequestStatus.OPEN);
        }

        // Extract branch info
        @SuppressWarnings("unchecked")
        Map<String, Object> head = (Map<String, Object>) prData.get("head");
        if (head != null) {
            pr.setSourceBranch((String) head.get("ref"));
            pr.setHeadCommitSha((String) head.get("sha"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> base = (Map<String, Object>) prData.get("base");
        if (base != null) {
            pr.setTargetBranch((String) base.get("ref"));
        }

        // Extract author info
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) prData.get("user");
        if (user != null) {
            pr.setAuthorUsername((String) user.get("login"));
            pr.setAuthorName((String) user.get("name"));
        }

        // Set mergeable flag
        Boolean mergeable = (Boolean) prData.get("mergeable");
        pr.setMergeable(mergeable);

        // Set timestamps
        String createdAtStr = (String) prData.get("created_at");
        if (createdAtStr != null && pr.getCreatedAt() == null) {
            pr.setCreatedAt(parseIsoTimestamp(createdAtStr));
        }

        String updatedAtStr = (String) prData.get("updated_at");
        if (updatedAtStr != null) {
            pr.setUpdatedAt(parseIsoTimestamp(updatedAtStr));
        }

        // Save PR
        pr = pullRequestRepository.save(pr);
        log.info("Saved PR: #{} - {}", prNumber, action);

        // Link PR to tasks if auto-linking is enabled
        if (integration.getAutoLinkEnabled()) {
            List<Task> linkedTasks = linkingService.linkPullRequestToTasks(pr);
            log.info("Linked PR #{} to {} tasks", prNumber, linkedTasks.size());

            // If PR was merged and auto-close is enabled, close linked tasks
            if (Boolean.TRUE.equals(merged) && integration.getAutoCloseOnMerge()) {
                // TODO: Implement auto-close logic
                // This would check which tasks should be closed and transition them
                log.debug("Auto-close on merge is enabled (implementation pending)");
            }
        }
    }

    private void handleGitHubCreateEvent(GitWebhookEvent event, Map<String, Object> payload) {
        log.debug("Handling GitHub create event");

        String refType = (String) payload.get("ref_type");
        if (!"branch".equals(refType)) {
            log.debug("Not a branch creation event, skipping");
            return;
        }

        GitIntegration integration = event.getGitIntegration();
        if (integration == null || !integration.getAutoLinkEnabled()) {
            return;
        }

        String branchName = (String) payload.get("ref");
        String baseBranch = (String) payload.get("master_branch");

        // Try to find task from branch name
        Task task = linkingService.findTaskByKey(branchName, integration.getProject().getId());
        if (task == null) {
            log.debug("No task found for branch: {}", branchName);
            return;
        }

        // Check if branch already exists
        Optional<GitBranch> existingBranch = branchRepository
                .findByGitIntegrationIdAndBranchName(integration.getId(), branchName);

        if (existingBranch.isPresent()) {
            log.debug("Branch already exists: {}", branchName);
            return;
        }

        // Create new branch
        GitBranch branch = new GitBranch(integration, task, branchName);
        branch.setBaseBranch(baseBranch);
        branch.setStatus(BranchStatus.ACTIVE);
        branch.setCreatedFromUi(false);

        branch = branchRepository.save(branch);
        log.info("Created branch: {} linked to task: {}", branchName, task.getKey());
    }

    private void handleGitHubDeleteEvent(GitWebhookEvent event, Map<String, Object> payload) {
        log.debug("Handling GitHub delete event");

        String refType = (String) payload.get("ref_type");
        if (!"branch".equals(refType)) {
            log.debug("Not a branch deletion event, skipping");
            return;
        }

        GitIntegration integration = event.getGitIntegration();
        if (integration == null) {
            return;
        }

        String branchName = (String) payload.get("ref");

        // Find and mark branch as deleted
        Optional<GitBranch> existingBranch = branchRepository
                .findByGitIntegrationIdAndBranchName(integration.getId(), branchName);

        if (existingBranch.isPresent()) {
            GitBranch branch = existingBranch.get();
            branch.setStatus(BranchStatus.DELETED);
            branch.setDeletedAt(LocalDateTime.now());
            branchRepository.save(branch);
            log.info("Marked branch as deleted: {}", branchName);
        }
    }

    // ==================== GitLab Event Handlers ====================

    private void processGitLabEvent(GitWebhookEvent event) {
        String eventType = event.getEventType();
        Map<String, Object> payload = event.getPayload();

        switch (eventType) {
            case "Push Hook" -> handleGitLabPushEvent(event, payload);
            case "Merge Request Hook" -> handleGitLabMergeRequestEvent(event, payload);
            default -> log.warn("Unsupported GitLab event type: {}", eventType);
        }
    }

    private void handleGitLabPushEvent(GitWebhookEvent event, Map<String, Object> payload) {
        log.debug("Handling GitLab push event");

        GitIntegration integration = event.getGitIntegration();
        if (integration == null || !integration.getAutoLinkEnabled()) {
            return;
        }

        String ref = (String) payload.get("ref");
        String branchName = ref != null && ref.startsWith("refs/heads/")
                ? ref.substring("refs/heads/".length())
                : ref;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");

        if (commits == null || commits.isEmpty()) {
            return;
        }

        log.info("Processing {} commits from GitLab push event", commits.size());

        for (Map<String, Object> commitData : commits) {
            try {
                processCommitFromGitLabPush(integration, commitData, branchName);
            } catch (Exception e) {
                log.error("Error processing GitLab commit: {}", commitData.get("id"), e);
            }
        }
    }

    private void processCommitFromGitLabPush(GitIntegration integration, Map<String, Object> commitData, String branchName) {
        String commitSha = (String) commitData.get("id");
        String message = (String) commitData.get("message");

        Optional<GitCommit> existingCommit = commitRepository
                .findByGitIntegrationIdAndCommitSha(integration.getId(), commitSha);

        if (existingCommit.isPresent()) {
            return;
        }

        GitCommit commit = new GitCommit(integration, commitSha, message);
        commit.setBranchName(branchName);

        @SuppressWarnings("unchecked")
        Map<String, Object> author = (Map<String, Object>) commitData.get("author");
        if (author != null) {
            commit.setAuthorName((String) author.get("name"));
            commit.setAuthorEmail((String) author.get("email"));
        }

        String timestamp = (String) commitData.get("timestamp");
        if (timestamp != null) {
            LocalDateTime dateTime = parseIsoTimestamp(timestamp);
            commit.setAuthorDate(dateTime);
            commit.setCommitterDate(dateTime);
        }

        String commitUrl = (String) commitData.get("url");
        commit.setCommitUrl(commitUrl);

        commit = commitRepository.save(commit);
        log.info("Created GitLab commit: {}", commitSha);

        if (integration.getAutoLinkEnabled()) {
            List<Task> linkedTasks = linkingService.linkCommitToTasks(commit);
            log.info("Linked GitLab commit {} to {} tasks", commitSha, linkedTasks.size());

            if (integration.getSmartCommitsEnabled() && !linkedTasks.isEmpty()) {
                try {
                    smartCommitService.processCommit(commit);
                } catch (Exception e) {
                    log.error("Error executing smart commits for {}", commitSha, e);
                }
            }
        }
    }

    private void handleGitLabMergeRequestEvent(GitWebhookEvent event, Map<String, Object> payload) {
        log.debug("Handling GitLab merge request event");
        // TODO: Implement GitLab MR handling (similar to GitHub PR)
        log.warn("GitLab merge request handling not yet implemented");
    }

    // ==================== Helper Methods ====================

    private LocalDateTime parseIsoTimestamp(String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (Exception e) {
            log.error("Error parsing timestamp: {}", timestamp, e);
            return LocalDateTime.now();
        }
    }
}
