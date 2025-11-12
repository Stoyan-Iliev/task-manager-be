package com.gradproject.taskmanager.modules.notification.event;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.Getter;


@Getter
public class TaskAssignedEvent extends NotificationEvent {

    private final User assignee;

    public TaskAssignedEvent(Object source, Task task, User assignee, User actor) {
        super(source, task, actor, NotificationType.TASK_ASSIGNED);
        this.assignee = assignee;
    }

    @Override
    public String getMessage() {
        return String.format("%s assigned %s to %s",
                getActor().getUsername(),
                getTask().getKey(),
                assignee.getUsername());
    }

    @Override
    public String getTitle() {
        return "Task Assigned";
    }
}
