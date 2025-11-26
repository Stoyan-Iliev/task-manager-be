package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.domain.GitWebhookEvent;


public interface GitWebhookProcessorService {

    /**
     * Process a webhook event asynchronously.
     * This method will parse the payload, create/update entities, link to tasks,
     * and trigger smart commit execution.
     *
     * @param event The webhook event to process
     */
    void processWebhookEvent(GitWebhookEvent event);

    /**
     * Retry processing a failed webhook event.
     *
     * @param event The webhook event to retry
     */
    void retryWebhookEvent(GitWebhookEvent event);
}
