package com.gradproject.taskmanager.modules.git.domain;

import com.gradproject.taskmanager.modules.git.domain.enums.LinkMethod;
import com.gradproject.taskmanager.modules.task.domain.Task;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Setter
@Getter
@Entity
@Table(name = "git_pr_tasks", uniqueConstraints = {
    @UniqueConstraint(name = "uk_git_pr_task", columnNames = {"git_pull_request_id", "task_id"})
})
public class GitPrTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_pull_request_id", nullable = false)
    private GitPullRequest gitPullRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_method", nullable = false, length = 50)
    private LinkMethod linkMethod;

    @Column(name = "closes_task")
    private Boolean closesTask = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (closesTask == null) closesTask = false;
    }

    public GitPrTask() {}

    public GitPrTask(GitPullRequest gitPullRequest, Task task, LinkMethod linkMethod, Boolean closesTask) {
        this.gitPullRequest = gitPullRequest;
        this.task = task;
        this.linkMethod = linkMethod;
        this.closesTask = closesTask;
    }
}
