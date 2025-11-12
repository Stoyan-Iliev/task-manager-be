package com.gradproject.taskmanager.modules.task.controller;

import com.gradproject.taskmanager.modules.task.dto.WatchersResponse;
import com.gradproject.taskmanager.modules.task.service.TaskWatcherService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.dto.PageResponse;
import com.gradproject.taskmanager.shared.dto.TaskSummary;
import com.gradproject.taskmanager.shared.dto.UserSummary;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/secure")
@RequiredArgsConstructor
@Slf4j
public class TaskWatcherController {

    private final TaskWatcherService watcherService;

    
    @PostMapping("/tasks/{taskId}/watch")
    public ResponseEntity<ApiResponse<Void>> watchTask(@PathVariable Long taskId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} watching task {}", currentUserId, taskId);
        watcherService.addWatcher(taskId, currentUserId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    
    @DeleteMapping("/tasks/{taskId}/watch")
    public ResponseEntity<ApiResponse<Void>> unwatchTask(@PathVariable Long taskId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} unwatching task {}", currentUserId, taskId);
        watcherService.removeWatcher(taskId, currentUserId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    
    @GetMapping("/tasks/{taskId}/watchers")
    public ResponseEntity<ApiResponse<WatchersResponse>> getWatchers(@PathVariable Long taskId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();

        List<UserSummary> watchers = watcherService.getTaskWatchers(taskId, currentUserId);
        int count = watcherService.getWatcherCount(taskId);
        boolean isWatching = watcherService.isWatching(taskId, currentUserId);

        WatchersResponse response = new WatchersResponse(watchers, count, isWatching);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PostMapping("/tasks/{taskId}/watchers/{userId}")
    public ResponseEntity<ApiResponse<Void>> addWatcher(
            @PathVariable Long taskId,
            @PathVariable Integer userId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} adding watcher {} to task {}", currentUserId, userId, taskId);
        watcherService.addWatcher(taskId, userId, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    
    @DeleteMapping("/tasks/{taskId}/watchers/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeWatcher(
            @PathVariable Long taskId,
            @PathVariable Integer userId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} removing watcher {} from task {}", currentUserId, userId, taskId);
        watcherService.removeWatcher(taskId, userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    
    @GetMapping("/users/me/watching")
    public ResponseEntity<ApiResponse<PageResponse<TaskSummary>>> getWatchedTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();

        PageResponse<TaskSummary> tasks = watcherService.getWatchedTasks(
            currentUserId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "addedAt"))
        );

        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    
    @DeleteMapping("/users/me/watching/completed")
    public ResponseEntity<ApiResponse<Void>> unwatchCompletedTasks() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        watcherService.unwatchCompletedTasks(currentUserId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
