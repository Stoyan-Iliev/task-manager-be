package com.gradproject.taskmanager.modules.task.service;

import com.gradproject.taskmanager.modules.activity.service.ActivityLogService;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.WorkLog;
import com.gradproject.taskmanager.modules.task.domain.WorkLogSource;
import com.gradproject.taskmanager.modules.task.dto.WorkLogRequest;
import com.gradproject.taskmanager.modules.task.dto.WorkLogResponse;
import com.gradproject.taskmanager.modules.task.dto.WorkLogSummary;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.modules.task.repository.WorkLogRepository;
import com.gradproject.taskmanager.shared.dto.UserSummary;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class WorkLogService {

    private final WorkLogRepository workLogRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ActivityLogService activityLogService;

    /**
     * Log time to a task.
     */
    @Transactional
    public WorkLogResponse logTime(Long taskId, WorkLogRequest request, Integer userId) {
        return logTime(taskId, request, userId, WorkLogSource.MANUAL);
    }

    /**
     * Log time to a task with specified source.
     */
    @Transactional
    public WorkLogResponse logTime(Long taskId, WorkLogRequest request, Integer userId, WorkLogSource source) {
        log.debug("Logging {} minutes to task {} by user {} via {}",
                request.timeSpentMinutes(), taskId, userId, source);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        verifyCanAccessTask(user, task.getProject());

        WorkLog workLog = WorkLog.builder()
                .task(task)
                .author(user)
                .timeSpentMinutes(request.timeSpentMinutes())
                .workDate(request.getWorkDateOrToday())
                .description(request.getTrimmedDescription())
                .source(source)
                .build();

        workLog = workLogRepository.save(workLog);

        // Update task's logged hours
        updateTaskLoggedHours(task);

        // Log activity
        activityLogService.logWorkLogged(task, workLog, user);

        log.info("User {} logged {} minutes to task {} (work log ID: {})",
                user.getUsername(), request.timeSpentMinutes(), task.getKey(), workLog.getId());

        return toResponse(workLog);
    }

    /**
     * Log time via smart commit.
     */
    @Transactional
    public WorkLogResponse logTimeFromSmartCommit(Long taskId, int minutes, String description, Integer userId) {
        WorkLogRequest request = new WorkLogRequest(minutes, LocalDate.now(), description);
        return logTime(taskId, request, userId, WorkLogSource.SMART_COMMIT);
    }

    /**
     * Update a work log entry.
     */
    @Transactional
    public WorkLogResponse updateWorkLog(Long workLogId, WorkLogRequest request, Integer userId) {
        log.debug("Updating work log {} by user {}", workLogId, userId);

        WorkLog workLog = workLogRepository.findById(workLogId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkLog", workLogId));

        if (!workLog.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("You can only edit your own work logs");
        }

        int oldMinutes = workLog.getTimeSpentMinutes();

        workLog.setTimeSpentMinutes(request.timeSpentMinutes());
        workLog.setWorkDate(request.getWorkDateOrToday());
        workLog.setDescription(request.getTrimmedDescription());

        workLog = workLogRepository.save(workLog);

        // Update task's logged hours if time changed
        if (oldMinutes != request.timeSpentMinutes()) {
            updateTaskLoggedHours(workLog.getTask());
        }

        log.info("User {} updated work log {}", userId, workLogId);

        return toResponse(workLog);
    }

    /**
     * Delete a work log entry.
     */
    @Transactional
    public void deleteWorkLog(Long workLogId, Integer userId) {
        log.debug("Deleting work log {} by user {}", workLogId, userId);

        WorkLog workLog = workLogRepository.findById(workLogId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkLog", workLogId));

        if (!workLog.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own work logs");
        }

        Task task = workLog.getTask();
        User user = workLog.getAuthor();

        // Log activity before deletion
        activityLogService.logWorkLogDeleted(task, workLog, user);

        workLogRepository.delete(workLog);

        // Update task's logged hours
        updateTaskLoggedHours(task);

        log.info("User {} deleted work log {}", userId, workLogId);
    }

    /**
     * Get all work logs for a task.
     */
    @Transactional(readOnly = true)
    public List<WorkLogResponse> getTaskWorkLogs(Long taskId, Integer userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        verifyCanAccessTask(user, task.getProject());

        return workLogRepository.findByTaskIdOrderByWorkDateDescCreatedAtDesc(taskId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get work logs for a task with pagination.
     */
    @Transactional(readOnly = true)
    public Page<WorkLogResponse> getTaskWorkLogs(Long taskId, Integer userId, Pageable pageable) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        verifyCanAccessTask(user, task.getProject());

        return workLogRepository.findByTaskIdOrderByWorkDateDescCreatedAtDesc(taskId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get a single work log.
     */
    @Transactional(readOnly = true)
    public WorkLogResponse getWorkLog(Long workLogId, Integer userId) {
        WorkLog workLog = workLogRepository.findById(workLogId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkLog", workLogId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        verifyCanAccessTask(user, workLog.getTask().getProject());

        return toResponse(workLog);
    }

    /**
     * Get work log summary for a task.
     */
    @Transactional(readOnly = true)
    public WorkLogSummary getTaskWorkLogSummary(Long taskId, Integer userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        verifyCanAccessTask(user, task.getProject());

        Integer totalMinutes = workLogRepository.getTotalTimeSpentForTask(taskId);
        long count = workLogRepository.countByTaskId(taskId);

        return new WorkLogSummary(
                taskId,
                totalMinutes,
                formatTimeSpent(totalMinutes),
                (int) count
        );
    }

    /**
     * Get my work logs (current user).
     */
    @Transactional(readOnly = true)
    public List<WorkLogResponse> getMyWorkLogs(Integer userId) {
        return workLogRepository.findByAuthorIdOrderByWorkDateDescCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get my work logs with pagination.
     */
    @Transactional(readOnly = true)
    public Page<WorkLogResponse> getMyWorkLogs(Integer userId, Pageable pageable) {
        return workLogRepository.findByAuthorIdOrderByWorkDateDescCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get my work logs within a date range.
     */
    @Transactional(readOnly = true)
    public List<WorkLogResponse> getMyWorkLogs(Integer userId, LocalDate startDate, LocalDate endDate) {
        return workLogRepository.findByAuthorIdAndDateRange(userId, startDate, endDate)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== Helper Methods ====================

    private void verifyCanAccessTask(User user, Project project) {
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You do not have access to this task");
        }
    }

    private void updateTaskLoggedHours(Task task) {
        Integer totalMinutes = workLogRepository.getTotalTimeSpentForTask(task.getId());
        BigDecimal hours = BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        task.setLoggedHours(hours);
        taskRepository.save(task);
    }

    private WorkLogResponse toResponse(WorkLog workLog) {
        return new WorkLogResponse(
                workLog.getId(),
                workLog.getTask().getId(),
                workLog.getTask().getKey(),
                toUserSummary(workLog.getAuthor()),
                workLog.getTimeSpentMinutes(),
                workLog.getTimeSpentFormatted(),
                workLog.getWorkDate(),
                workLog.getDescription(),
                workLog.getSource(),
                workLog.getCreatedAt(),
                workLog.getUpdatedAt()
        );
    }

    private UserSummary toUserSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarUrl()
        );
    }

    private String formatTimeSpent(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return "0m";
        }

        int remaining = minutes;
        StringBuilder sb = new StringBuilder();

        int weeks = remaining / 2400;
        if (weeks > 0) {
            sb.append(weeks).append("w ");
            remaining %= 2400;
        }

        int days = remaining / 480;
        if (days > 0) {
            sb.append(days).append("d ");
            remaining %= 480;
        }

        int hours = remaining / 60;
        if (hours > 0) {
            sb.append(hours).append("h ");
            remaining %= 60;
        }

        if (remaining > 0) {
            sb.append(remaining).append("m");
        }

        return sb.toString().trim();
    }
}
