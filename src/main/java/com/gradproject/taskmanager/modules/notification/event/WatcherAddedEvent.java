package com.gradproject.taskmanager.modules.notification.event;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.Getter;


@Getter
public class WatcherAddedEvent extends NotificationEvent {

    private final User watcher;

    public WatcherAddedEvent(Object source, Task task, User watcher, User addedBy) {
        super(source, task, addedBy, NotificationType.WATCHER_ADDED);
        this.watcher = watcher;
    }

    @Override
    public String getMessage() {
        return String.format("%s added you as a watcher to %s",
                getActor().getUsername(),
                getTask().getKey());
    }

    @Override
    public String getTitle() {
        return "Added as Watcher";
    }
}
