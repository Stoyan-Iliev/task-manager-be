package com.gradproject.taskmanager.modules.notification.event;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.Getter;


@Getter
public class TaskStatusChangedEvent extends NotificationEvent {

    private final TaskStatus oldStatus;
    private final TaskStatus newStatus;

    public TaskStatusChangedEvent(Object source, Task task, TaskStatus oldStatus, TaskStatus newStatus, User actor) {
        super(source, task, actor, NotificationType.STATUS_CHANGED);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    @Override
    public String getMessage() {
        return String.format("%s changed status of %s from %s to %s",
                getActor().getUsername(),
                getTask().getKey(),
                oldStatus.getName(),
                newStatus.getName());
    }

    @Override
    public String getTitle() {
        return "Status Changed";
    }
}
