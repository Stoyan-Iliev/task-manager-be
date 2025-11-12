package com.gradproject.taskmanager.modules.notification.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.dto.NotificationResponse;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.task.domain.Comment;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import com.gradproject.taskmanager.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Set;


public interface NotificationService {

    

    
    PageResponse<NotificationResponse> getUserNotifications(Integer userId, Pageable pageable);

    
    PageResponse<NotificationResponse> getUnreadNotifications(Integer userId, Pageable pageable);

    
    long getUnreadCount(Integer userId);

    

    
    void markAsRead(Long notificationId, Integer userId);

    
    void markAllAsRead(Integer userId);

    

    
    void notifyTaskCreated(Task task, User creator);

    
    void notifyStatusChanged(Task task, TaskStatus oldStatus, TaskStatus newStatus, User actor);

    
    void notifyTaskAssigned(Task task, User assignee, User actor);

    
    void notifyTaskUnassigned(Task task, User previousAssignee, User actor);

    
    void notifyPriorityChanged(Task task, TaskPriority oldPriority, TaskPriority newPriority, User actor);

    
    void notifyDueDateChanged(Task task, LocalDate oldDueDate, LocalDate newDueDate, User actor);

    
    void notifyCommentAdded(Task task, Comment comment, User commenter);

    
    void notifyMentioned(Task task, Comment comment, Set<String> mentionedUsernames, User commenter);

    
    void notifyWatcherAdded(Task task, User watcher, User addedBy);

    

    
    int cleanupOldNotifications(int daysOld);
}
