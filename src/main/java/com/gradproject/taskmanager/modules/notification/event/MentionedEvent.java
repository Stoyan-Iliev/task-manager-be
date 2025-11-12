package com.gradproject.taskmanager.modules.notification.event;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Comment;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.Getter;

import java.util.Set;


@Getter
public class MentionedEvent extends NotificationEvent {

    private final Comment comment;
    private final Set<String> mentionedUsernames;

    public MentionedEvent(Object source, Task task, Comment comment, Set<String> mentionedUsernames, User commenter) {
        super(source, task, commenter, NotificationType.MENTIONED);
        this.comment = comment;
        this.mentionedUsernames = mentionedUsernames;
    }

    @Override
    public String getMessage() {
        String preview = comment.getContent().length() > 50
                ? comment.getContent().substring(0, 50) + "..."
                : comment.getContent();

        return String.format("%s mentioned you in %s: %s",
                getActor().getUsername(),
                getTask().getKey(),
                preview);
    }

    @Override
    public String getTitle() {
        return "You Were Mentioned";
    }
}
