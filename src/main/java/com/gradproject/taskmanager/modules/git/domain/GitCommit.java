package com.gradproject.taskmanager.modules.git.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Setter
@Getter
@Entity
@Table(name = "git_commits", uniqueConstraints = {
    @UniqueConstraint(name = "uk_git_commit_sha", columnNames = {"git_integration_id", "commit_sha"})
})
public class GitCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_integration_id", nullable = false)
    private GitIntegration gitIntegration;

    @Column(name = "commit_sha", nullable = false, length = 40)
    private String commitSha;

    @Column(name = "parent_sha", length = 40)
    private String parentSha;

    @Column(name = "branch_name")
    private String branchName;

    
    @Column(name = "author_name")
    private String authorName;

    @Column(name = "author_email")
    private String authorEmail;

    @Column(name = "author_date")
    private LocalDateTime authorDate;

    @Column(name = "committer_name")
    private String committerName;

    @Column(name = "committer_email")
    private String committerEmail;

    @Column(name = "committer_date")
    private LocalDateTime committerDate;

    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "message_body", columnDefinition = "TEXT")
    private String messageBody;

    
    @Column(name = "commit_type", length = 50)
    private String commitType;

    @Column(name = "lines_added")
    private Integer linesAdded;

    @Column(name = "lines_deleted")
    private Integer linesDeleted;

    @Column(name = "files_changed")
    private Integer filesChanged;

    
    @Column(name = "commit_url", columnDefinition = "TEXT")
    private String commitUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public GitCommit() {}

    public GitCommit(GitIntegration gitIntegration, String commitSha, String message) {
        this.gitIntegration = gitIntegration;
        this.commitSha = commitSha;
        this.message = message;
    }
}
