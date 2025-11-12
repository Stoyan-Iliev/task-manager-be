package com.gradproject.taskmanager.modules.task.domain;

import com.gradproject.taskmanager.modules.organization.domain.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "labels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "organization")
public class Label {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    
    @Column(nullable = false, length = 50)
    private String name;

    
    @Column(nullable = false, length = 7)
    private String color;

    
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Integer createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (color == null) {
            color = "#3b82f6"; 
        }
    }

    
    public boolean hasValidColor() {
        return color != null && color.matches("^#[0-9A-Fa-f]{6}$");
    }
}
