package com.gradproject.taskmanager.modules.git.repository;

import com.gradproject.taskmanager.modules.git.domain.GitWebhookEvent;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GitWebhookEventRepository extends JpaRepository<GitWebhookEvent, Long> {

    List<GitWebhookEvent> findByGitIntegrationId(Long integrationId);

    Page<GitWebhookEvent> findByGitIntegrationId(Long integrationId, Pageable pageable);

    List<GitWebhookEvent> findByProvider(GitProvider provider);

    List<GitWebhookEvent> findByEventType(String eventType);

    List<GitWebhookEvent> findByProcessedFalse();

    @Query("SELECT gwe FROM GitWebhookEvent gwe WHERE gwe.processed = false AND gwe.retryCount < :maxRetries ORDER BY gwe.receivedAt ASC")
    List<GitWebhookEvent> findUnprocessedWithRetries(@Param("maxRetries") Integer maxRetries, Pageable pageable);

    @Query("SELECT gwe FROM GitWebhookEvent gwe WHERE gwe.gitIntegration.id = :integrationId AND gwe.receivedAt >= :since ORDER BY gwe.receivedAt DESC")
    List<GitWebhookEvent> findRecentByIntegration(@Param("integrationId") Long integrationId,
                                                   @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(gwe) FROM GitWebhookEvent gwe WHERE gwe.processed = false")
    long countUnprocessed();

    Page<GitWebhookEvent> findByGitIntegrationIdOrderByReceivedAtDesc(Long integrationId, Pageable pageable);
}
