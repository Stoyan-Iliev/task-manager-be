package com.gradproject.taskmanager.modules.notification.event;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import lombok.Getter;


@Getter
public class TaskPriorityChangedEvent extends NotificationEvent {

    private final TaskPriority oldPriority;
    private final TaskPriority newPriority;

    public TaskPriorityChangedEvent(Object source, Task task, TaskPriority oldPriority, TaskPriority newPriority, User actor) {
        super(source, task, actor, NotificationType.PRIORITY_CHANGED);
        this.oldPriority = oldPriority;
        this.newPriority = newPriority;
    }

    @Override
    public String getMessage() {
        return String.format("%s changed priority of %s from %s to %s",
                getActor().getUsername(),
                getTask().getKey(),
                oldPriority.name(),
                newPriority.name());
    }

    @Override
    public String getTitle() {
        return "Priority Changed";
    }
}
