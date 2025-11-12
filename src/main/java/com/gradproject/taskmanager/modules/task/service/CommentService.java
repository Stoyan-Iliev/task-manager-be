package com.gradproject.taskmanager.modules.task.service;

import com.gradproject.taskmanager.modules.activity.service.ActivityLogService;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.event.CommentAddedEvent;
import com.gradproject.taskmanager.modules.notification.event.MentionedEvent;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.task.domain.Comment;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.dto.CommentRequest;
import com.gradproject.taskmanager.modules.task.dto.CommentResponse;
import com.gradproject.taskmanager.modules.task.repository.CommentRepository;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.BusinessRuleViolationException;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.CommentMapper;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final CommentMapper mapper;
    private final ActivityLogService activityLogService;
    private final TaskWatcherService watcherService;
    private final ApplicationEventPublisher eventPublisher;

    
    @Transactional
    public CommentResponse addComment(Long taskId, CommentRequest request, Integer userId) {
        log.debug("Adding comment to task {} by user {}", taskId, userId);

        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        verifyCanAccessTask(user, task.getProject());

        
        Comment comment = mapper.fromRequest(request);
        comment.setTask(task);
        comment.setUser(user);

        
        if (request.isReply()) {
            Comment parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", request.parentCommentId()));

            
            if (!parent.getTask().getId().equals(taskId)) {
                throw new BusinessRuleViolationException("Parent comment must belong to the same task");
            }

            
            if (parent.isReply()) {
                throw new BusinessRuleViolationException(
                        "Cannot reply to a reply. Maximum thread depth is 1. Reply to the top-level comment instead.");
            }

            comment.setParentComment(parent);
        }

        
        comment = commentRepository.save(comment);

        
        activityLogService.logCommentAdded(task, comment, user);

        
        watcherService.autoWatchOnComment(task, user);

        
        List<String> mentions = watcherService.extractMentions(comment.getContent());
        if (!mentions.isEmpty()) {
            watcherService.autoWatchOnMention(task, new java.util.HashSet<>(mentions));
        }

        
        eventPublisher.publishEvent(new CommentAddedEvent(this, task, comment, user));

        
        if (!mentions.isEmpty()) {
            eventPublisher.publishEvent(new MentionedEvent(this, task, comment, new java.util.HashSet<>(mentions), user));
        }

        log.info("User {} added comment {} to task {} (parent: {})",
                user.getUsername(), comment.getId(), task.getKey(), request.parentCommentId());

        
        CommentResponse response = mapper.toResponse(comment);
        return new CommentResponse(
                response.id(),
                response.taskId(),
                mapper.toUserSummary(user),
                response.parentCommentId(),
                response.content(),
                response.edited(),
                response.createdAt(),
                response.updatedAt(),
                new ArrayList<>()  
        );
    }

    
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest request, Integer userId) {
        log.debug("Updating comment {} by user {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        
        if (!comment.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only edit your own comments");
        }

        
        comment.setContent(request.getTrimmedContent());
        comment = commentRepository.save(comment);

        log.info("User {} edited comment {}", userId, commentId);

        
        List<CommentResponse> replies = new ArrayList<>();
        if (!comment.isReply()) {
            replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId)
                    .stream()
                    .map(reply -> {
                        CommentResponse replyResponse = mapper.toResponse(reply);
                        return new CommentResponse(
                                replyResponse.id(),
                                replyResponse.taskId(),
                                mapper.toUserSummary(reply.getUser()),
                                replyResponse.parentCommentId(),
                                replyResponse.content(),
                                replyResponse.edited(),
                                replyResponse.createdAt(),
                                replyResponse.updatedAt(),
                                new ArrayList<>()
                        );
                    })
                    .collect(Collectors.toList());
        }

        
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                mapper.toUserSummary(comment.getUser()),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                comment.getContent(),
                comment.getEdited(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replies
        );
    }

    
    @Transactional
    public void deleteComment(Long commentId, Integer userId) {
        log.debug("Deleting comment {} by user {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        
        if (!comment.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own comments");
        }

        
        long replyCount = 0;
        if (!comment.isReply()) {
            replyCount = commentRepository.countByParentCommentId(commentId);
        }

        
        Task task = comment.getTask();
        User user = comment.getUser();

        
        activityLogService.logCommentDeleted(task, comment, user);

        commentRepository.delete(comment);

        log.info("User {} deleted comment {} (with {} replies)", userId, commentId, replyCount);
    }

    
    @Transactional(readOnly = true)
    public List<CommentResponse> getTaskComments(Long taskId, Integer userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        verifyCanAccessTask(user, task.getProject());

        
        List<Comment> topLevel = commentRepository.findTopLevelCommentsByTaskId(taskId);

        
        return topLevel.stream()
                .map(comment -> {
                    
                    List<CommentResponse> replies = commentRepository
                            .findByParentCommentIdOrderByCreatedAtAsc(comment.getId())
                            .stream()
                            .map(reply -> new CommentResponse(
                                    reply.getId(),
                                    reply.getTask().getId(),
                                    mapper.toUserSummary(reply.getUser()),
                                    reply.getParentComment().getId(),
                                    reply.getContent(),
                                    reply.getEdited(),
                                    reply.getCreatedAt(),
                                    reply.getUpdatedAt(),
                                    new ArrayList<>()  
                            ))
                            .collect(Collectors.toList());

                    
                    return new CommentResponse(
                            comment.getId(),
                            comment.getTask().getId(),
                            mapper.toUserSummary(comment.getUser()),
                            null,  
                            comment.getContent(),
                            comment.getEdited(),
                            comment.getCreatedAt(),
                            comment.getUpdatedAt(),
                            replies
                    );
                })
                .collect(Collectors.toList());
    }

    
    @Transactional(readOnly = true)
    public CommentResponse getComment(Long commentId, Integer userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        verifyCanAccessTask(user, comment.getTask().getProject());

        
        List<CommentResponse> replies = new ArrayList<>();
        if (!comment.isReply()) {
            replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId)
                    .stream()
                    .map(reply -> new CommentResponse(
                            reply.getId(),
                            reply.getTask().getId(),
                            mapper.toUserSummary(reply.getUser()),
                            reply.getParentComment().getId(),
                            reply.getContent(),
                            reply.getEdited(),
                            reply.getCreatedAt(),
                            reply.getUpdatedAt(),
                            new ArrayList<>()
                    ))
                    .collect(Collectors.toList());
        }

        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                mapper.toUserSummary(comment.getUser()),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                comment.getContent(),
                comment.getEdited(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replies
        );
    }

    

    
    private void verifyCanAccessTask(User user, Project project) {
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You do not have access to this task");
        }
    }
}
