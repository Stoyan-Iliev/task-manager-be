package com.gradproject.taskmanager.modules.task.domain;

import com.gradproject.taskmanager.modules.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "work_logs", indexes = {
    @Index(name = "idx_work_logs_task_id", columnList = "task_id"),
    @Index(name = "idx_work_logs_author_id", columnList = "author_id"),
    @Index(name = "idx_work_logs_work_date", columnList = "work_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"task", "author"})
public class WorkLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "time_spent_minutes", nullable = false)
    private Integer timeSpentMinutes;

    @Column(name = "work_date", nullable = false)
    @Builder.Default
    private LocalDate workDate = LocalDate.now();

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private WorkLogSource source = WorkLogSource.MANUAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (workDate == null) {
            workDate = LocalDate.now();
        }
        if (source == null) {
            source = WorkLogSource.MANUAL;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get time spent formatted as a human-readable string.
     * Examples: "2h 30m", "1h", "45m", "1w 2d 4h"
     */
    public String getTimeSpentFormatted() {
        if (timeSpentMinutes == null || timeSpentMinutes <= 0) {
            return "0m";
        }

        int remaining = timeSpentMinutes;
        StringBuilder sb = new StringBuilder();

        // Weeks (40 hours = 2400 minutes)
        int weeks = remaining / 2400;
        if (weeks > 0) {
            sb.append(weeks).append("w ");
            remaining %= 2400;
        }

        // Days (8 hours = 480 minutes)
        int days = remaining / 480;
        if (days > 0) {
            sb.append(days).append("d ");
            remaining %= 480;
        }

        // Hours (60 minutes)
        int hours = remaining / 60;
        if (hours > 0) {
            sb.append(hours).append("h ");
            remaining %= 60;
        }

        // Minutes
        if (remaining > 0) {
            sb.append(remaining).append("m");
        }

        return sb.toString().trim();
    }
}
