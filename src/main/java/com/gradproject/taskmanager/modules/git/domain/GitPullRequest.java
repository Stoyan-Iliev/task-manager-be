package com.gradproject.taskmanager.modules.git.domain;

import com.gradproject.taskmanager.modules.git.domain.enums.ChecksStatus;
import com.gradproject.taskmanager.modules.git.domain.enums.PullRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Setter
@Getter
@Entity
@Table(name = "git_pull_requests", uniqueConstraints = {
    @UniqueConstraint(name = "uk_git_pr_number", columnNames = {"git_integration_id", "pr_number"})
})
public class GitPullRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_integration_id", nullable = false)
    private GitIntegration gitIntegration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_branch_id")
    private GitBranch gitBranch;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "pr_title", nullable = false, length = 500)
    private String prTitle;

    @Column(name = "pr_description", columnDefinition = "TEXT")
    private String prDescription;

    @Column(name = "pr_url", columnDefinition = "TEXT")
    private String prUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PullRequestStatus status;

    
    @Column(name = "source_branch")
    private String sourceBranch;

    @Column(name = "target_branch")
    private String targetBranch;

    @Column(name = "head_commit_sha", length = 40)
    private String headCommitSha;

    
    @Column(name = "author_username")
    private String authorUsername;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "author_email")
    private String authorEmail;

    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reviewers", columnDefinition = "JSONB")
    private List<Object> reviewers = new ArrayList<>();

    @Column(name = "approvals_count")
    private Integer approvalsCount = 0;

    @Column(name = "required_approvals")
    private Integer requiredApprovals;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "checks_status", length = 20)
    private ChecksStatus checksStatus;

    @Column(name = "checks_count")
    private Integer checksCount = 0;

    @Column(name = "checks_passed")
    private Integer checksPassed = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checks_details", columnDefinition = "JSONB")
    private List<Object> checksDetails = new ArrayList<>();

    
    private Boolean mergeable;

    private Boolean merged = false;

    @Column(name = "merged_at")
    private LocalDateTime mergedAt;

    @Column(name = "merged_by")
    private String mergedBy;

    @Column(name = "merge_commit_sha", length = 40)
    private String mergeCommitSha;

    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (approvalsCount == null) approvalsCount = 0;
        if (checksCount == null) checksCount = 0;
        if (checksPassed == null) checksPassed = 0;
        if (merged == null) merged = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public GitPullRequest() {}

    public GitPullRequest(GitIntegration gitIntegration, Integer prNumber, String prTitle, PullRequestStatus status) {
        this.gitIntegration = gitIntegration;
        this.prNumber = prNumber;
        this.prTitle = prTitle;
        this.status = status;
    }
}
