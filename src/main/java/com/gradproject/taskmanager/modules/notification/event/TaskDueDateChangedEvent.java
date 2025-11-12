package com.gradproject.taskmanager.modules.notification.event;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@Getter
public class TaskDueDateChangedEvent extends NotificationEvent {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final LocalDate oldDueDate;
    private final LocalDate newDueDate;

    public TaskDueDateChangedEvent(Object source, Task task, LocalDate oldDueDate, LocalDate newDueDate, User actor) {
        super(source, task, actor, NotificationType.DUE_DATE_CHANGED);
        this.oldDueDate = oldDueDate;
        this.newDueDate = newDueDate;
    }

    @Override
    public String getMessage() {
        String oldDateStr = oldDueDate != null ? oldDueDate.format(DATE_FORMATTER) : "None";
        String newDateStr = newDueDate != null ? newDueDate.format(DATE_FORMATTER) : "None";

        return String.format("%s changed due date of %s from %s to %s",
                getActor().getUsername(),
                getTask().getKey(),
                oldDateStr,
                newDateStr);
    }

    @Override
    public String getTitle() {
        return "Due Date Changed";
    }
}
