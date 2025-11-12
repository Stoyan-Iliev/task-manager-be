package com.gradproject.taskmanager.modules.notification.event;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;


@Getter
public abstract class NotificationEvent extends ApplicationEvent {

    private final Task task;
    private final User actor;
    private final NotificationType type;

    protected NotificationEvent(Object source, Task task, User actor, NotificationType type) {
        super(source);
        this.task = task;
        this.actor = actor;
        this.type = type;
    }

    
    public abstract String getMessage();

    
    public String getTitle() {
        return "Task Update";
    }
}
