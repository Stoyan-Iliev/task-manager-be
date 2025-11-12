package com.gradproject.taskmanager.modules.notification.event;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.Getter;


@Getter
public class TaskCreatedEvent extends NotificationEvent {

    public TaskCreatedEvent(Object source, Task task, User creator) {
        super(source, task, creator, NotificationType.TASK_CREATED);
    }

    @Override
    public String getMessage() {
        return String.format("%s created task %s",
                getActor().getUsername(),
                getTask().getKey());
    }

    @Override
    public String getTitle() {
        return "New Task";
    }
}
