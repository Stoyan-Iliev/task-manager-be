package com.gradproject.taskmanager.modules.task.service;

import com.gradproject.taskmanager.infrastructure.config.properties.StorageProperties;
import com.gradproject.taskmanager.infrastructure.storage.S3Service;
import com.gradproject.taskmanager.modules.activity.service.ActivityLogService;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.task.domain.Attachment;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.dto.AttachmentDownload;
import com.gradproject.taskmanager.modules.task.dto.AttachmentResponse;
import com.gradproject.taskmanager.modules.task.repository.AttachmentRepository;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.dto.UserSummary;
import com.gradproject.taskmanager.shared.exception.BadRequestException;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final StorageProperties storageProperties;
    private final PermissionService permissionService;
    private final ActivityLogService activityLogService;

    
    @Transactional
    public AttachmentResponse uploadAttachment(Long taskId, MultipartFile file, Integer userId) {
        log.debug("Uploading attachment to task {}: {} ({} bytes)", taskId, file.getOriginalFilename(), file.getSize());

        
        validateFile(file);

        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        verifyCanManageTask(user, task.getProject());

        
        String uuid = UUID.randomUUID().toString();
        String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
        String extension = getFileExtension(file.getOriginalFilename());

        String storagePath = String.format(
                "org-%d/task-%d/%s-%s%s",
                task.getOrganization().getId(),
                task.getId(),
                uuid,
                sanitizedFilename,
                extension
        );

        
        s3Service.uploadFile(file, storagePath);

        
        String thumbnailPath = null;
        if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
            thumbnailPath = s3Service.generateThumbnail(file, storagePath);
        }

        
        Attachment attachment = Attachment.builder()
                .task(task)
                .uploadedBy(user)
                .filename(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .storagePath(storagePath)
                .thumbnailPath(thumbnailPath)
                .build();

        attachment = attachmentRepository.save(attachment);

        
        activityLogService.logAttachmentAdded(task, attachment.getId(), attachment.getFilename(), attachment.getFileSizeBytes(), user);

        log.info("User {} uploaded attachment {} to task {} ({} bytes)",
                user.getUsername(), attachment.getId(), task.getKey(), file.getSize());

        return toResponse(attachment, user);
    }

    
    @Transactional(readOnly = true)
    public AttachmentDownload downloadAttachment(Long attachmentId, Integer userId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        verifyCanAccessTask(user, attachment.getTask().getProject());

        byte[] data = s3Service.downloadFile(attachment.getStoragePath());

        log.info("User {} downloaded attachment {} ({} bytes)", userId, attachmentId, data.length);

        return new AttachmentDownload(
                attachment.getFilename(),
                attachment.getMimeType(),
                data
        );
    }

    
    @Transactional
    public void deleteAttachment(Long attachmentId, Integer userId) {
        log.debug("Deleting attachment {} by user {}", attachmentId, userId);

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        verifyCanManageTask(user, attachment.getTask().getProject());

        
        Task task = attachment.getTask();
        Long attachmentIdValue = attachment.getId();
        String filename = attachment.getFilename();

        
        activityLogService.logAttachmentDeleted(task, attachmentIdValue, filename, user);

        
        s3Service.deleteFile(attachment.getStoragePath());
        if (attachment.hasThumbnail()) {
            s3Service.deleteFile(attachment.getThumbnailPath());
        }

        
        attachmentRepository.delete(attachment);

        log.info("User {} deleted attachment {} from task {}", userId, attachmentId, attachment.getTask().getKey());
    }

    
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getTaskAttachments(Long taskId, Integer userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        verifyCanAccessTask(user, task.getProject());

        return attachmentRepository.findByTaskIdOrderByUploadedAtDesc(taskId)
                .stream()
                .map(attachment -> toResponse(attachment, attachment.getUploadedBy()))
                .collect(Collectors.toList());
    }

    

    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > storageProperties.maxFileSizeBytes()) {
            throw new BadRequestException("File size exceeds limit of " +
                    (storageProperties.maxFileSizeBytes() / (1024 * 1024)) + " MB");
        }

        if (file.getContentType() == null || !storageProperties.isAllowedMimeType(file.getContentType())) {
            throw new BadRequestException("File type not allowed: " + file.getContentType());
        }
    }

    
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "file";
        }
        
        String nameOnly = filename.contains(".") ?
                filename.substring(0, filename.lastIndexOf('.')) : filename;
        return nameOnly.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    
    private AttachmentResponse toResponse(Attachment attachment, User uploadedBy) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getTask().getId(),
                attachment.getFilename(),
                attachment.getMimeType(),
                attachment.getFileSizeBytes(),
                attachment.getFormattedFileSize(),
                attachment.getStoragePath(),
                attachment.getThumbnailPath(),
                new UserSummary(uploadedBy.getId(), uploadedBy.getUsername(), uploadedBy.getEmail(), uploadedBy.getFirstName(), uploadedBy.getLastName(), uploadedBy.getAvatarUrl()),
                attachment.getUploadedAt(),
                attachment.isImage(),
                attachment.hasThumbnail()
        );
    }

    
    private void verifyCanAccessTask(User user, Project project) {
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You do not have access to this task");
        }
    }

    
    private void verifyCanManageTask(User user, Project project) {
        if (!permissionService.canManageTasks(user, project)) {
            throw new UnauthorizedException("You do not have permission to manage this task");
        }
    }
}
