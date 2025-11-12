package com.gradproject.taskmanager.modules.git.domain;

import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Setter
@Getter
@Entity
@Table(name = "git_webhook_events")
public class GitWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_integration_id")
    private GitIntegration gitIntegration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GitProvider provider;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_action", length = 100)
    private String eventAction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> payload = new HashMap<>();

    @Column(length = 500)
    private String signature;

    @Column(nullable = false)
    private Boolean processed = false;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
        if (processed == null) processed = false;
        if (retryCount == null) retryCount = 0;
    }

    public GitWebhookEvent() {}

    public GitWebhookEvent(GitProvider provider, String eventType, Map<String, Object> payload) {
        this.provider = provider;
        this.eventType = eventType;
        this.payload = payload;
    }
}
