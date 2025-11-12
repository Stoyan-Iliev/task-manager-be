package com.gradproject.taskmanager.modules.notification.event;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.Getter;


@Getter
public class TaskUnassignedEvent extends NotificationEvent {

    private final User previousAssignee;

    public TaskUnassignedEvent(Object source, Task task, User previousAssignee, User actor) {
        super(source, task, actor, NotificationType.TASK_UNASSIGNED);
        this.previousAssignee = previousAssignee;
    }

    @Override
    public String getMessage() {
        return String.format("%s unassigned %s from %s",
                getActor().getUsername(),
                getTask().getKey(),
                previousAssignee.getUsername());
    }

    @Override
    public String getTitle() {
        return "Task Unassigned";
    }
}
