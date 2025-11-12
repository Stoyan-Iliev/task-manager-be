package com.gradproject.taskmanager.modules.git.domain;

import com.gradproject.taskmanager.modules.git.domain.enums.LinkMethod;
import com.gradproject.taskmanager.modules.task.domain.Task;
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
@Table(name = "git_commit_tasks", uniqueConstraints = {
    @UniqueConstraint(name = "uk_git_commit_task", columnNames = {"git_commit_id", "task_id"})
})
public class GitCommitTask {

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
    @Column(name = "link_method", nullable = false, length = 50)
    private LinkMethod linkMethod;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "smart_commands", columnDefinition = "JSONB")
    private List<Object> smartCommands = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public GitCommitTask() {}

    public GitCommitTask(GitCommit gitCommit, Task task, LinkMethod linkMethod) {
        this.gitCommit = gitCommit;
        this.task = task;
        this.linkMethod = linkMethod;
    }
}
