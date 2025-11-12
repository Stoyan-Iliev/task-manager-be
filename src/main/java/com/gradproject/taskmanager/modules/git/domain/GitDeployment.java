package com.gradproject.taskmanager.modules.git.domain;

import com.gradproject.taskmanager.modules.git.domain.enums.DeploymentEnvironment;
import com.gradproject.taskmanager.modules.git.domain.enums.DeploymentStatus;
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
@Table(name = "git_deployments")
public class GitDeployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_integration_id", nullable = false)
    private GitIntegration gitIntegration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeploymentEnvironment environment;

    @Column(name = "environment_url", columnDefinition = "TEXT")
    private String environmentUrl;

    @Column(name = "commit_sha", nullable = false, length = 40)
    private String commitSha;

    @Column(name = "commit_message", columnDefinition = "TEXT")
    private String commitMessage;

    @Column(name = "deployment_id", length = 100)
    private String deploymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "deployment_status", nullable = false, length = 20)
    private DeploymentStatus deploymentStatus;

    @Column(name = "deployed_by")
    private String deployedBy;

    @Column(name = "deployment_tool", length = 100)
    private String deploymentTool;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public GitDeployment() {}

    public GitDeployment(GitIntegration gitIntegration, DeploymentEnvironment environment,
                         String commitSha, DeploymentStatus status, LocalDateTime startedAt) {
        this.gitIntegration = gitIntegration;
        this.environment = environment;
        this.commitSha = commitSha;
        this.deploymentStatus = status;
        this.startedAt = startedAt;
    }
}
