package com.gradproject.taskmanager.modules.task.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.shared.dto.PageResponse;
import com.gradproject.taskmanager.shared.dto.TaskSummary;
import com.gradproject.taskmanager.shared.dto.UserSummary;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;


public interface TaskWatcherService {

    

    
    void addWatcher(Long taskId, Integer userId, Integer addedBy);

    
    void removeWatcher(Long taskId, Integer userId, Integer removedBy);

    
    boolean isWatching(Long taskId, Integer userId);

    
    List<UserSummary> getTaskWatchers(Long taskId, Integer requestingUserId);

    
    int getWatcherCount(Long taskId);

    

    
    PageResponse<TaskSummary> getWatchedTasks(Integer userId, Pageable pageable);

    
    void unwatchCompletedTasks(Integer userId);

    

    
    void autoWatchOnCreate(Task task, User creator);

    
    void autoWatchOnAssign(Task task, User assignee);

    
    void autoWatchOnComment(Task task, User commenter);

    
    void autoWatchOnMention(Task task, Set<String> mentionedUsernames);

    

    
    void addWatchers(Long taskId, List<Integer> userIds, Integer addedBy);

    
    void removeWatchers(Long taskId, List<Integer> userIds, Integer removedBy);

    

    
    List<String> extractMentions(String content);
}
