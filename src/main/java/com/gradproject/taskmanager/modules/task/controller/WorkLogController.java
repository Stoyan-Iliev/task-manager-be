package com.gradproject.taskmanager.modules.task.controller;

import com.gradproject.taskmanager.modules.task.dto.WorkLogRequest;
import com.gradproject.taskmanager.modules.task.dto.WorkLogResponse;
import com.gradproject.taskmanager.modules.task.dto.WorkLogSummary;
import com.gradproject.taskmanager.modules.task.service.WorkLogService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.dto.PageResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/secure")
@RequiredArgsConstructor
@Slf4j
public class WorkLogController {

    private final WorkLogService workLogService;

    /**
     * Log time to a task.
     */
    @PostMapping("/tasks/{taskId}/worklogs")
    public ResponseEntity<ApiResponse<WorkLogResponse>> logTime(
            @PathVariable Long taskId,
            @Valid @RequestBody WorkLogRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        WorkLogResponse response = workLogService.logTime(taskId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Get all work logs for a task.
     */
    @GetMapping("/tasks/{taskId}/worklogs")
    public ResponseEntity<ApiResponse<List<WorkLogResponse>>> getTaskWorkLogs(@PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<WorkLogResponse> workLogs = workLogService.getTaskWorkLogs(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(workLogs));
    }

    /**
     * Get work logs for a task with pagination.
     */
    @GetMapping("/tasks/{taskId}/worklogs/page")
    public ResponseEntity<ApiResponse<PageResponse<WorkLogResponse>>> getTaskWorkLogsPaged(
            @PathVariable Long taskId,
            @PageableDefault(size = 20) Pageable pageable) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Page<WorkLogResponse> page = workLogService.getTaskWorkLogs(taskId, userId, pageable);
        PageResponse<WorkLogResponse> response = new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get work log summary for a task.
     */
    @GetMapping("/tasks/{taskId}/worklogs/summary")
    public ResponseEntity<ApiResponse<WorkLogSummary>> getTaskWorkLogSummary(@PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        WorkLogSummary summary = workLogService.getTaskWorkLogSummary(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get a single work log.
     */
    @GetMapping("/worklogs/{workLogId}")
    public ResponseEntity<ApiResponse<WorkLogResponse>> getWorkLog(@PathVariable Long workLogId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        WorkLogResponse response = workLogService.getWorkLog(workLogId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update a work log.
     */
    @PutMapping("/worklogs/{workLogId}")
    public ResponseEntity<ApiResponse<WorkLogResponse>> updateWorkLog(
            @PathVariable Long workLogId,
            @Valid @RequestBody WorkLogRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        WorkLogResponse response = workLogService.updateWorkLog(workLogId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete a work log.
     */
    @DeleteMapping("/worklogs/{workLogId}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkLog(@PathVariable Long workLogId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        workLogService.deleteWorkLog(workLogId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * Get my work logs.
     */
    @GetMapping("/worklogs/mine")
    public ResponseEntity<ApiResponse<List<WorkLogResponse>>> getMyWorkLogs() {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<WorkLogResponse> workLogs = workLogService.getMyWorkLogs(userId);
        return ResponseEntity.ok(ApiResponse.success(workLogs));
    }

    /**
     * Get my work logs with pagination.
     */
    @GetMapping("/worklogs/mine/page")
    public ResponseEntity<ApiResponse<PageResponse<WorkLogResponse>>> getMyWorkLogsPaged(
            @PageableDefault(size = 20) Pageable pageable) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Page<WorkLogResponse> page = workLogService.getMyWorkLogs(userId, pageable);
        PageResponse<WorkLogResponse> response = new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get my work logs within a date range.
     */
    @GetMapping("/worklogs/mine/range")
    public ResponseEntity<ApiResponse<List<WorkLogResponse>>> getMyWorkLogsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<WorkLogResponse> workLogs = workLogService.getMyWorkLogs(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(workLogs));
    }
}
