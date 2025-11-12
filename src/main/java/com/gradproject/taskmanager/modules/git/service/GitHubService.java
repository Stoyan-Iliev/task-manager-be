package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.GitIntegration;


public interface GitHubService {

    
    ConnectionTestResult testConnection(String repositoryOwner, String repositoryName, String accessToken);

    
    WebhookResult createWebhook(GitIntegration integration);

    
    boolean removeWebhook(GitIntegration integration);

    
    boolean verifyWebhookSignature(String payload, String signature, String webhookSecret);

    
    SyncResult syncBranches(GitIntegration integration);

    
    SyncResult syncCommits(GitIntegration integration, String since);

    
    SyncResult syncPullRequests(GitIntegration integration, String state);

    
    Object getCommitDetails(GitIntegration integration, String commitSha);

    
    record ConnectionTestResult(
        boolean success,
        String message,
        RepositoryPermissions permissions
    ) {}

    
    record RepositoryPermissions(
        boolean canRead,
        boolean canWrite,
        boolean canAdmin,
        boolean canCreateWebhook
    ) {}

    
    record WebhookResult(
        boolean success,
        String webhookId,
        String webhookUrl,
        String message
    ) {}

    
    record SyncResult(
        boolean success,
        int itemsFetched,
        int itemsCreated,
        int itemsUpdated,
        String message
    ) {}
}
