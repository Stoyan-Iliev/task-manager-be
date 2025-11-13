package com.gradproject.taskmanager.modules.task.service;

import com.gradproject.taskmanager.modules.activity.domain.ActionType;
import com.gradproject.taskmanager.modules.activity.domain.EntityType;
import com.gradproject.taskmanager.modules.activity.service.ActivityLogService;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.event.WatcherAddedEvent;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskWatcher;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.modules.task.repository.TaskWatcherRepository;
import com.gradproject.taskmanager.shared.dto.PageResponse;
import com.gradproject.taskmanager.shared.dto.TaskSummary;
import com.gradproject.taskmanager.shared.dto.UserSummary;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskWatcherServiceImpl implements TaskWatcherService {

    private final TaskWatcherRepository watcherRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final PermissionService permissionService;
    private final ActivityLogService activityLogService;
    private final ApplicationEventPublisher eventPublisher;

    
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_-]+)");

    

    @Override
    public void addWatcher(Long taskId, Integer userId, Integer addedBy) {
        log.debug("Adding watcher userId={} to taskId={} by userId={}", userId, taskId, addedBy);

        
        Task task = taskRepo.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!permissionService.canAccessProject(user, task.getProject())) {
            throw new UnauthorizedException("User " + userId + " cannot access task " + taskId);
        }

        
        if (watcherRepo.existsByTaskIdAndUserId(taskId, userId)) {
            log.debug("User {} already watching task {}", userId, taskId);
            return;
        }

        
        TaskWatcher watcher = TaskWatcher.builder()
            .task(task)
            .user(user)
            .addedBy(addedBy)
            .addedAt(LocalDateTime.now())
            .build();

        watcherRepo.save(watcher);

        log.info("Added watcher: userId={} to taskId={} (added by userId={})", userId, taskId, addedBy);

        
        User addedByUser = userRepo.findById(addedBy).orElseThrow();
        activityLogService.logActivityWithMetadata(
            task.getOrganization(),
            task.getProject(),
            task,
            EntityType.TASK,
            taskId,
            ActionType.WATCHER_ADDED,
            addedByUser,
            Map.of(
                "watcher_user_id", userId,
                "watcher_username", user.getUsername(),
                "added_by_self", userId.equals(addedBy)
            )
        );

        
        eventPublisher.publishEvent(new WatcherAddedEvent(this, task, user, addedByUser));
    }

    @Override
    public void removeWatcher(Long taskId, Integer userId, Integer removedBy) {
        log.debug("Removing watcher userId={} from taskId={} by userId={}", userId, taskId, removedBy);

        
        if (!userId.equals(removedBy)) {
            Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

            User removingUser = userRepo.findById(removedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", removedBy));

            if (!permissionService.canManageMembers(removingUser, task.getProject())) {
                throw new UnauthorizedException("Only admins can remove other users as watchers");
            }
        }

        
        if (!watcherRepo.existsByTaskIdAndUserId(taskId, userId)) {
            log.debug("User {} not watching task {}", userId, taskId);
            return; 
        }

        watcherRepo.deleteByTaskIdAndUserId(taskId, userId);

        log.info("Removed watcher: userId={} from taskId={} (removed by userId={})", userId, taskId, removedBy);

        
        Task task = taskRepo.findById(taskId).orElseThrow();
        User user = userRepo.findById(userId).orElseThrow();
        User removedByUser = userRepo.findById(removedBy).orElseThrow();

        activityLogService.logActivityWithMetadata(
            task.getOrganization(),
            task.getProject(),
            task,
            EntityType.TASK,
            taskId,
            ActionType.WATCHER_REMOVED,
            removedByUser,
            Map.of(
                "watcher_user_id", userId,
                "watcher_username", user.getUsername(),
                "removed_by_self", userId.equals(removedBy)
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWatching(Long taskId, Integer userId) {
        return watcherRepo.existsByTaskIdAndUserId(taskId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> getTaskWatchers(Long taskId, Integer requestingUserId) {
        
        Task task = taskRepo.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User requestingUser = userRepo.findById(requestingUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", requestingUserId));

        if (!permissionService.canAccessProject(requestingUser, task.getProject())) {
            throw new UnauthorizedException("Cannot access this task");
        }

        List<TaskWatcher> watchers = watcherRepo.findByTaskId(taskId);

        return watchers.stream()
            .map(tw -> new UserSummary(
                tw.getUser().getId(),
                tw.getUser().getUsername(),
                tw.getUser().getEmail(),
                tw.getUser().getFirstName(),
                tw.getUser().getLastName(),
                tw.getUser().getAvatarUrl()
            ))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public int getWatcherCount(Long taskId) {
        return watcherRepo.countByTaskId(taskId);
    }

    

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskSummary> getWatchedTasks(Integer userId, Pageable pageable) {
        Page<TaskWatcher> watcherPage = watcherRepo.findByUserIdOrderByAddedAtDesc(userId, pageable);

        List<TaskSummary> taskSummaries = watcherPage.getContent().stream()
            .map(tw -> mapTaskToSummary(tw.getTask()))
            .collect(Collectors.toList());

        return new PageResponse<>(
            taskSummaries,
            watcherPage.getNumber(),
            watcherPage.getSize(),
            watcherPage.getTotalElements(),
            watcherPage.getTotalPages()
        );
    }

    @Override
    public void unwatchCompletedTasks(Integer userId) {
        List<TaskWatcher> watchers = watcherRepo.findByUserId(userId);

        List<Long> completedTaskIds = watchers.stream()
            .filter(tw -> tw.getTask().isDone())
            .map(tw -> tw.getTask().getId())
            .collect(Collectors.toList());

        for (Long taskId : completedTaskIds) {
            removeWatcher(taskId, userId, userId);
        }

        log.info("Unwatched {} completed tasks for user {}", completedTaskIds.size(), userId);
    }

    

    @Override
    public void autoWatchOnCreate(Task task, User creator) {
        log.debug("Auto-watch on create: taskId={}, creatorId={}", task.getId(), creator.getId());
        addWatcher(task.getId(), creator.getId(), creator.getId());
    }

    @Override
    public void autoWatchOnAssign(Task task, User assignee) {
        if (assignee == null) {
            log.debug("No assignee to auto-watch for task {}", task.getId());
            return;
        }

        log.debug("Auto-watch on assign: taskId={}, assigneeId={}", task.getId(), assignee.getId());
        addWatcher(task.getId(), assignee.getId(), assignee.getId());
    }

    @Override
    public void autoWatchOnComment(Task task, User commenter) {
        log.debug("Auto-watch on comment: taskId={}, commenterId={}", task.getId(), commenter.getId());
        addWatcher(task.getId(), commenter.getId(), commenter.getId());
    }

    @Override
    public void autoWatchOnMention(Task task, Set<String> mentionedUsernames) {
        if (mentionedUsernames == null || mentionedUsernames.isEmpty()) {
            return;
        }

        log.debug("Auto-watch on mention: taskId={}, mentioned={}", task.getId(), mentionedUsernames);

        for (String username : mentionedUsernames) {
            userRepo.findByUsername(username).ifPresent(user -> {
                
                if (permissionService.canAccessProject(user, task.getProject())) {
                    addWatcher(task.getId(), user.getId(), task.getReporter().getId());
                    log.debug("Auto-watched user {} (mentioned in task {})", username, task.getId());
                } else {
                    log.debug("User {} mentioned but cannot access task {}", username, task.getId());
                }
            });
        }
    }

    

    @Override
    public void addWatchers(Long taskId, List<Integer> userIds, Integer addedBy) {
        for (Integer userId : userIds) {
            try {
                addWatcher(taskId, userId, addedBy);
            } catch (Exception e) {
                log.error("Failed to add watcher userId={} to taskId={}: {}", userId, taskId, e.getMessage());
            }
        }
    }

    @Override
    public void removeWatchers(Long taskId, List<Integer> userIds, Integer removedBy) {
        for (Integer userId : userIds) {
            try {
                removeWatcher(taskId, userId, removedBy);
            } catch (Exception e) {
                log.error("Failed to remove watcher userId={} from taskId={}: {}", userId, taskId, e.getMessage());
            }
        }
    }

    

    @Override
    public List<String> extractMentions(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        Set<String> mentions = new HashSet<>();

        while (matcher.find()) {
            mentions.add(matcher.group(1)); 
        }

        return new ArrayList<>(mentions);
    }

    

    private TaskSummary mapTaskToSummary(Task task) {
        return new TaskSummary(
            task.getId(),
            task.getKey(),
            task.getTitle(),
            task.getStatus().getName(),
            task.getPriority(),
            task.getAssignee() != null ? new UserSummary(
                task.getAssignee().getId(),
                task.getAssignee().getUsername(),
                task.getAssignee().getEmail(),
                task.getAssignee().getFirstName(),
                task.getAssignee().getLastName(),
                task.getAssignee().getAvatarUrl()
            ) : null,
            task.getDueDate(),
            task.getUpdatedAt()
        );
    }
}
