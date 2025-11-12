package com.gradproject.taskmanager.modules.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@Entity
@Table(name = "task_statuses")
public class TaskStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 100)
    private String name;

    
    @Column(length = 7)
    private String color;

    
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCategory category;

    
    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public TaskStatus() {}

    public TaskStatus(Project project, String name, String color, Integer orderIndex, StatusCategory category, Boolean isDefault) {
        this.project = project;
        this.name = name;
        this.color = color;
        this.orderIndex = orderIndex;
        this.category = category;
        this.isDefault = isDefault;
    }
}
