package com.gradproject.taskmanager.modules.task.controller;

import com.gradproject.taskmanager.modules.task.dto.CommentRequest;
import com.gradproject.taskmanager.modules.task.dto.CommentResponse;
import com.gradproject.taskmanager.modules.task.service.CommentService;
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
public class CommentController {

    private final CommentService commentService;

    
    @PostMapping("/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CommentRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        CommentResponse response = commentService.addComment(taskId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping("/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getTaskComments(@PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<CommentResponse> comments = commentService.getTaskComments(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    
    @GetMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> getComment(@PathVariable Long commentId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        CommentResponse response = commentService.getComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        CommentResponse response = commentService.updateComment(commentId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
