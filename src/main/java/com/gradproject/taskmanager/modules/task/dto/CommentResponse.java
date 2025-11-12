package com.gradproject.taskmanager.modules.task.dto;

import com.gradproject.taskmanager.shared.dto.UserSummary;

import java.time.LocalDateTime;
import java.util.List;


public record CommentResponse(
        Long id,
        Long taskId,
        UserSummary user,
        Long parentCommentId,
        String content,
        Boolean edited,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> replies  
) {
    
    public boolean isReply() {
        return parentCommentId != null;
    }

    
    public boolean wasEdited() {
        return edited != null && edited;
    }

    
    public boolean hasReplies() {
        return replies != null && !replies.isEmpty();
    }

    
    public int getReplyCount() {
        return replies != null ? replies.size() : 0;
    }
}
