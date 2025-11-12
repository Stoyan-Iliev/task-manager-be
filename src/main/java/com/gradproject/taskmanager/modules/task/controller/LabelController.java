package com.gradproject.taskmanager.modules.task.controller;

import com.gradproject.taskmanager.modules.task.dto.LabelRequest;
import com.gradproject.taskmanager.modules.task.dto.LabelResponse;
import com.gradproject.taskmanager.modules.task.dto.TaskLabelRequest;
import com.gradproject.taskmanager.modules.task.service.LabelService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/secure")
@RequiredArgsConstructor
@Slf4j
public class LabelController {

    private final LabelService labelService;

    
    @PostMapping("/organizations/{orgId}/labels")
    public ResponseEntity<ApiResponse<LabelResponse>> createLabel(
            @PathVariable Long orgId,
            @Valid @RequestBody LabelRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        LabelResponse response = labelService.createLabel(orgId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping("/organizations/{orgId}/labels")
    public ResponseEntity<ApiResponse<List<LabelResponse>>> listLabels(@PathVariable Long orgId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<LabelResponse> labels = labelService.listOrganizationLabels(orgId, userId);
        return ResponseEntity.ok(ApiResponse.success(labels));
    }

    
    @GetMapping("/labels/{labelId}")
    public ResponseEntity<ApiResponse<LabelResponse>> getLabel(@PathVariable Long labelId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        LabelResponse response = labelService.getLabel(labelId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PutMapping("/labels/{labelId}")
    public ResponseEntity<ApiResponse<LabelResponse>> updateLabel(
            @PathVariable Long labelId,
            @Valid @RequestBody LabelRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        LabelResponse response = labelService.updateLabel(labelId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/labels/{labelId}")
    public ResponseEntity<ApiResponse<Void>> deleteLabel(@PathVariable Long labelId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        labelService.deleteLabel(labelId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    
    @PostMapping("/tasks/{taskId}/labels")
    public ResponseEntity<ApiResponse<Void>> addLabelToTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskLabelRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        labelService.addLabelToTask(taskId, request.labelId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    
    @DeleteMapping("/tasks/{taskId}/labels/{labelId}")
    public ResponseEntity<ApiResponse<Void>> removeLabelFromTask(
            @PathVariable Long taskId,
            @PathVariable Long labelId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        labelService.removeLabelFromTask(taskId, labelId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    
    @GetMapping("/tasks/{taskId}/labels")
    public ResponseEntity<ApiResponse<List<LabelResponse>>> getTaskLabels(@PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<LabelResponse> labels = labelService.getTaskLabels(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(labels));
    }
}
