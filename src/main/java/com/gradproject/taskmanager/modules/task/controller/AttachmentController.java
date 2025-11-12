package com.gradproject.taskmanager.modules.task.controller;

import com.gradproject.taskmanager.modules.task.dto.AttachmentDownload;
import com.gradproject.taskmanager.modules.task.dto.AttachmentResponse;
import com.gradproject.taskmanager.modules.task.service.AttachmentService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/secure")
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {

    private final AttachmentService attachmentService;

    
    @PostMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) {
        Integer userId = SecurityUtils.getCurrentUserId();
        AttachmentResponse response = attachmentService.uploadAttachment(taskId, file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getTaskAttachments(@PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<AttachmentResponse> attachments = attachmentService.getTaskAttachments(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(attachments));
    }

    
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long attachmentId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        AttachmentDownload download = attachmentService.downloadAttachment(attachmentId, userId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(download.getFileSize()))
                .body(download.data());
    }

    
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(@PathVariable Long attachmentId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        attachmentService.deleteAttachment(attachmentId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
