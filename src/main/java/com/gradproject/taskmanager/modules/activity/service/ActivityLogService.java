package com.gradproject.taskmanager.modules.activity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradproject.taskmanager.modules.activity.domain.ActionType;
import com.gradproject.taskmanager.modules.activity.domain.ActivityLog;
import com.gradproject.taskmanager.modules.activity.domain.EntityType;
import com.gradproject.taskmanager.modules.activity.repository.ActivityLogRepository;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.task.domain.Comment;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.WorkLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    
    @Transactional
    public void logStatusChange(Task task, TaskStatus oldStatus, TaskStatus newStatus, User user) {
        log.debug("Logging status change for task {}: {} → {}", task.getKey(), oldStatus.getName(), newStatus.getName());

        int nextVersion = getNextVersionNumber(EntityType.TASK, task.getId());

        ActivityLog activityLog = ActivityLog.builder()
                .organization(task.getOrganization())
                .project(task.getProject())
                .task(task)
                .entityType(EntityType.TASK)
                .entityId(task.getId())
                .action(ActionType.STATUS_CHANGED)
                .user(user)
                .fieldName("status")
                .oldValue(toJsonString(Map.of("id", oldStatus.getId(), "name", oldStatus.getName())))
                .newValue(toJsonString(Map.of("id", newStatus.getId(), "name", newStatus.getName())))
                .versionNumber(nextVersion)
                .build();

        activityLogRepository.save(activityLog);
    }

    
    @Transactional
    public void logAssignment(Task task, User oldAssignee, User newAssignee, User actingUser) {
        int nextVersion = getNextVersionNumber(EntityType.TASK, task.getId());

        ActionType action = newAssignee != null ? ActionType.ASSIGNED : ActionType.UNASSIGNED;

        String oldValue = oldAssignee != null ?
                toJsonString(Map.of("id", oldAssignee.getId(), "username", oldAssignee.getUsername())) : null;

        String newValue = newAssignee != null ?
                toJsonString(Map.of("id", newAssignee.getId(), "username", newAssignee.getUsername())) : null;

        ActivityLog activityLog = ActivityLog.builder()
                .organization(task.getOrganization())
                .project(task.getProject())
                .task(task)
                .entityType(EntityType.TASK)
                .entityId(task.getId())
                .action(action)
                .user(actingUser)
                .fieldName("assignee")
                .oldValue(oldValue)
                .newValue(newValue)
                .versionNumber(nextVersion)
                .build();

        activityLogRepository.save(activityLog);
    }

    
    @Transactional
    public void logCommentAdded(Task task, Comment comment, User user) {
        int nextVersion = getNextVersionNumber(EntityType.COMMENT, comment.getId());

        ActivityLog activityLog = ActivityLog.builder()
                .organization(task.getOrganization())
                .project(task.getProject())
                .task(task)
                .entityType(EntityType.COMMENT)
                .entityId(comment.getId())
                .action(ActionType.COMMENT_ADDED)
                .user(user)
                .metadata(toJsonString(Map.of(
                        "commentId", comment.getId(),
                        "isReply", comment.isReply(),
                        "contentLength", comment.getContent().length()
                )))
                .versionNumber(nextVersion)
                .build();

        activityLogRepository.save(activityLog);
    }

    
    @Transactional
    public void logCommentDeleted(Task task, Comment comment, User user) {
        int nextVersion = getNextVersionNumber(EntityType.COMMENT, comment.getId());

        ActivityLog activityLog = ActivityLog.builder()
                .organization(task.getOrganization())
                .project(task.getProject())
                .task(task)
                .entityType(EntityType.COMMENT)
                .entityId(comment.getId())
                .action(ActionType.COMMENT_DELETED)
                .user(user)
                .versionNumber(nextVersion)
                .build();

        activityLogRepository.save(activityLog);
    }

    
    @Transactional
    public void logAttachmentAdded(Task task, Long attachmentId, String filename, Long fileSize, User user) {
        int nextVersion = getNextVersionNumber(EntityType.ATTACHMENT, attachmentId);

        ActivityLog activityLog = ActivityLog.builder()
                .organization(task.getOrganization())
                .project(task.getProject())
                .task(task)
                .entityType(EntityType.ATTACHMENT)
                .entityId(attachmentId)
                .action(ActionType.ATTACHMENT_ADDED)
                .user(user)
                .metadata(toJsonString(Map.of(
                        "filename", filename,
                        "fileSize", fileSize
                )))
                .versionNumber(nextVersion)
                .build();

        activityLogRepository.save(activityLog);
    }

    
    @Transactional
    public void logAttachmentDeleted(Task task, Long attachmentId, String filename, User user) {
        int nextVersion = getNextVersionNumber(EntityType.ATTACHMENT, attachmentId);

        ActivityLog activityLog = ActivityLog.builder()
                .organization(task.getOrganization())
                .project(task.getProject())
                .task(task)
                .entityType(EntityType.ATTACHMENT)
                .entityId(attachmentId)
                .action(ActionType.ATTACHMENT_DELETED)
                .user(user)
                .metadata(toJsonString(Map.of("filename", filename)))
                .versionNumber(nextVersion)
                .build();

        activityLogRepository.save(activityLog);
    }

    
    @Transactional
    public void logTaskCreated(Task task, User user) {
        ActivityLog activityLog = ActivityLog.builder()
                .organization(task.getOrganization())
                .project(task.getProject())
                .task(task)
                .entityType(EntityType.TASK)
                .entityId(task.getId())
                .action(ActionType.CREATED)
                .user(user)
                .metadata(toJsonString(Map.of(
                        "title", task.getTitle(),
                        "type", task.getType().name(),
                        "priority", task.getPriority().name()
                )))
                .versionNumber(1)  
                .build();

        activityLogRepository.save(activityLog);
    }

    /**
     * Log work time added to a task.
     */
    @Transactional
    public void logWorkLogged(Task task, WorkLog workLog, User user) {
        int nextVersion = getNextVersionNumber(EntityType.WORK_LOG, workLog.getId());

        ActivityLog activityLog = ActivityLog.builder()
                .organization(task.getOrganization())
                .project(task.getProject())
                .task(task)
                .entityType(EntityType.WORK_LOG)
                .entityId(workLog.getId())
                .action(ActionType.WORK_LOGGED)
                .user(user)
                .metadata(toJsonString(Map.of(
                        "workLogId", workLog.getId(),
                        "timeSpentMinutes", workLog.getTimeSpentMinutes(),
                        "timeSpentFormatted", workLog.getTimeSpentFormatted(),
                        "workDate", workLog.getWorkDate().toString(),
                        "source", workLog.getSource().name()
                )))
                .versionNumber(nextVersion)
                .build();

        activityLogRepository.save(activityLog);
    }

    /**
     * Log work log deleted.
     */
    @Transactional
    public void logWorkLogDeleted(Task task, WorkLog workLog, User user) {
        int nextVersion = getNextVersionNumber(EntityType.WORK_LOG, workLog.getId());

        ActivityLog activityLog = ActivityLog.builder()
                .organization(task.getOrganization())
                .project(task.getProject())
                .task(task)
                .entityType(EntityType.WORK_LOG)
                .entityId(workLog.getId())
                .action(ActionType.WORK_LOG_DELETED)
                .user(user)
                .metadata(toJsonString(Map.of(
                        "timeSpentMinutes", workLog.getTimeSpentMinutes(),
                        "workDate", workLog.getWorkDate().toString()
                )))
                .versionNumber(nextVersion)
                .build();

        activityLogRepository.save(activityLog);
    }

    @Transactional
    public void logActivity(ActivityLog log) {
        activityLogRepository.save(log);
    }

    
    @Transactional
    public void logActivityWithMetadata(
            com.gradproject.taskmanager.modules.organization.domain.Organization organization,
            com.gradproject.taskmanager.modules.project.domain.Project project,
            Task task,
            EntityType entityType,
            Long entityId,
            ActionType action,
            User user,
            Map<String, Object> metadata) {

        int nextVersion = getNextVersionNumber(entityType, entityId);

        ActivityLog activityLog = ActivityLog.builder()
                .organization(organization)
                .project(project)
                .task(task)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .user(user)
                .metadata(toJsonString(metadata))
                .versionNumber(nextVersion)
                .build();

        activityLogRepository.save(activityLog);
    }

    

    
    public List<ActivityLog> getTaskActivity(Long taskId, Integer limit) {
        List<ActivityLog> logs = activityLogRepository.findByTaskIdOrderByTimestampDesc(taskId);
        if (limit != null && limit > 0 && logs.size() > limit) {
            return logs.subList(0, limit);
        }
        return logs;
    }

    
    public List<ActivityLog> getProjectActivity(Long projectId, Integer limit) {
        List<ActivityLog> logs = activityLogRepository.findByProjectIdOrderByTimestampDesc(projectId);
        if (limit != null && limit > 0 && logs.size() > limit) {
            return logs.subList(0, limit);
        }
        return logs;
    }

    
    public List<ActivityLog> getOrganizationActivity(Long organizationId, Integer limit) {
        List<ActivityLog> logs = activityLogRepository.findByOrganizationIdOrderByTimestampDesc(organizationId);
        if (limit != null && limit > 0 && logs.size() > limit) {
            return logs.subList(0, limit);
        }
        return logs;
    }

    
    public List<ActivityLog> getEntityHistory(EntityType entityType, Long entityId) {
        return activityLogRepository.findByEntityTypeAndEntityIdOrderByVersionNumberAsc(entityType, entityId);
    }

    
    public List<ActivityLog> getEntityStateAtVersion(EntityType entityType, Long entityId, Integer version) {
        return activityLogRepository.findEntityStateAtVersion(entityType, entityId, version);
    }

    

    
    private int getNextVersionNumber(EntityType entityType, Long entityId) {
        return activityLogRepository
                .findMaxVersionNumber(entityType, entityId)
                .map(v -> v + 1)
                .orElse(1);
    }

    
    private String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
