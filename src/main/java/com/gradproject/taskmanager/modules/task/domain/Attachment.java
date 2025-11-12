package com.gradproject.taskmanager.modules.task.domain;

import com.gradproject.taskmanager.modules.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"task", "uploadedBy"})
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    
    @Column(nullable = false)
    private String filename;

    
    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    
    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    
    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    
    public boolean hasThumbnail() {
        return thumbnailPath != null && !thumbnailPath.isBlank();
    }

    
    public String getFormattedFileSize() {
        if (fileSizeBytes == null) {
            return "0 B";
        }
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        }
        if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.2f KB", fileSizeBytes / 1024.0);
        }
        if (fileSizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSizeBytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", fileSizeBytes / (1024.0 * 1024 * 1024));
    }

    
    public String getFileExtension() {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
