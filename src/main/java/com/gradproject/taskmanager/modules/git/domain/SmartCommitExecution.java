package com.gradproject.taskmanager.modules.git.domain;

import com.gradproject.taskmanager.modules.activity.domain.ActivityLog;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.git.domain.enums.SmartCommitCommandType;
import com.gradproject.taskmanager.modules.task.domain.Task;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Setter
@Getter
@Entity
@Table(name = "smart_commit_executions")
public class SmartCommitExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_commit_id", nullable = false)
    private GitCommit gitCommit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 50)
    private SmartCommitCommandType commandType;

    @Column(name = "command_text", nullable = false, columnDefinition = "TEXT")
    private String commandText;

    @Column(nullable = false)
    private Boolean executed = false;

    @Column(name = "execution_error", columnDefinition = "TEXT")
    private String executionError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by")
    private User executedBy;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_log_id")
    private ActivityLog activityLog;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (executed == null) executed = false;
    }

    public SmartCommitExecution() {}

    public SmartCommitExecution(GitCommit gitCommit, Task task, SmartCommitCommandType commandType, String commandText) {
        this.gitCommit = gitCommit;
        this.task = task;
        this.commandType = commandType;
        this.commandText = commandText;
    }
}
