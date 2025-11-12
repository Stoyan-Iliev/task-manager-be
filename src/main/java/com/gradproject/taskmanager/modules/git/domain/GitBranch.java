package com.gradproject.taskmanager.modules.git.domain;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.git.domain.enums.BranchStatus;
import com.gradproject.taskmanager.modules.task.domain.Task;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Setter
@Getter
@Entity
@Table(name = "git_branches", uniqueConstraints = {
    @UniqueConstraint(name = "uk_git_branch_name", columnNames = {"git_integration_id", "branch_name"})
})
public class GitBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_integration_id", nullable = false)
    private GitIntegration gitIntegration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(name = "branch_ref", length = 500)
    private String branchRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BranchStatus status = BranchStatus.ACTIVE;

    @Column(name = "created_from_ui")
    private Boolean createdFromUi = false;

    @Column(name = "head_commit_sha", length = 40)
    private String headCommitSha;

    @Column(name = "base_branch")
    private String baseBranch;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "merged_at")
    private LocalDateTime mergedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = BranchStatus.ACTIVE;
        if (createdFromUi == null) createdFromUi = false;
    }

    public GitBranch() {}

    public GitBranch(GitIntegration gitIntegration, Task task, String branchName) {
        this.gitIntegration = gitIntegration;
        this.task = task;
        this.branchName = branchName;
    }
}
