package com.gradproject.taskmanager.modules.task.dto;

import com.gradproject.taskmanager.shared.dto.UserSummary;

import java.time.LocalDateTime;


public record AttachmentResponse(
        Long id,
        Long taskId,
        String filename,
        String mimeType,
        Long fileSizeBytes,
        String formattedFileSize,
        String storagePath,
        String thumbnailPath,
        UserSummary uploadedBy,
        LocalDateTime uploadedAt,
        boolean isImage,
        boolean hasThumbnail
) {
}
