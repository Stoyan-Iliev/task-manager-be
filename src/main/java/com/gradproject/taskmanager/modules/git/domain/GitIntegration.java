package com.gradproject.taskmanager.modules.git.domain;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Setter
@Getter
@Entity
@Table(name = "git_integrations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_git_integration_repo", columnNames = {"organization_id", "repository_full_name"})
})
public class GitIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GitProvider provider;

    @Column(name = "repository_url", nullable = false, length = 500)
    private String repositoryUrl;

    @Column(name = "repository_owner", nullable = false)
    private String repositoryOwner;

    @Column(name = "repository_name", nullable = false)
    private String repositoryName;

    @Column(name = "repository_full_name", nullable = false, length = 500)
    private String repositoryFullName;

    
    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "TEXT")
    private String accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted", columnDefinition = "TEXT")
    private String refreshTokenEncrypted;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    
    @Column(name = "webhook_id", length = 100)
    private String webhookId;

    @Column(name = "webhook_secret_encrypted", columnDefinition = "TEXT")
    private String webhookSecretEncrypted;

    @Column(name = "webhook_url", columnDefinition = "TEXT")
    private String webhookUrl;

    
    @Column(name = "auto_link_enabled")
    private Boolean autoLinkEnabled = true;

    @Column(name = "smart_commits_enabled")
    private Boolean smartCommitsEnabled = true;

    @Column(name = "auto_close_on_merge")
    private Boolean autoCloseOnMerge = true;

    @Column(name = "branch_prefix", length = 50)
    private String branchPrefix;

    
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (autoLinkEnabled == null) autoLinkEnabled = true;
        if (smartCommitsEnabled == null) smartCommitsEnabled = true;
        if (autoCloseOnMerge == null) autoCloseOnMerge = true;
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public GitIntegration() {}

    public GitIntegration(Organization organization, Project project, GitProvider provider,
                          String repositoryUrl, String repositoryFullName, User createdBy) {
        this.organization = organization;
        this.project = project;
        this.provider = provider;
        this.repositoryUrl = repositoryUrl;
        this.repositoryFullName = repositoryFullName;
        this.createdBy = createdBy;
    }
}
