package com.gradproject.taskmanager.modules.task.domain;

import com.gradproject.taskmanager.modules.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"task", "user", "parentComment"})
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    
    @Column(nullable = false)
    @Builder.Default
    private Boolean edited = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (edited == null) {
            edited = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        edited = true;
    }

    
    public boolean isReply() {
        return parentComment != null;
    }

    
    public boolean canHaveReplies() {
        return !isReply();
    }

    
    public int getThreadDepth() {
        return isReply() ? 1 : 0;
    }
}
