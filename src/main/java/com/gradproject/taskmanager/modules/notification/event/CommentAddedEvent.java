package com.gradproject.taskmanager.modules.notification.event;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Comment;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.Getter;


@Getter
public class CommentAddedEvent extends NotificationEvent {

    private final Comment comment;

    public CommentAddedEvent(Object source, Task task, Comment comment, User commenter) {
        super(source, task, commenter, NotificationType.COMMENT_ADDED);
        this.comment = comment;
    }

    @Override
    public String getMessage() {
        String preview = comment.getContent().length() > 50
                ? comment.getContent().substring(0, 50) + "..."
                : comment.getContent();

        return String.format("%s commented on %s: %s",
                getActor().getUsername(),
                getTask().getKey(),
                preview);
    }

    @Override
    public String getTitle() {
        return "New Comment";
    }
}
