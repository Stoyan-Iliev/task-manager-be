package com.gradproject.taskmanager.modules.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record CommentRequest(
        @NotBlank(message = "Comment content is required")
        @Size(min = 1, max = 5000, message = "Comment must be between 1 and 5000 characters")
        String content,

        
        Long parentCommentId
) {
    
    public boolean isReply() {
        return parentCommentId != null;
    }

    
    public String getTrimmedContent() {
        return content != null ? content.trim() : null;
    }
}
