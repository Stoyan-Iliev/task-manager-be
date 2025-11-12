package com.gradproject.taskmanager.modules.task.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationMemberRepository;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.task.domain.Label;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskLabel;
import com.gradproject.taskmanager.modules.task.dto.LabelRequest;
import com.gradproject.taskmanager.modules.task.dto.LabelResponse;
import com.gradproject.taskmanager.modules.task.repository.LabelRepository;
import com.gradproject.taskmanager.modules.task.repository.TaskLabelRepository;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.BusinessRuleViolationException;
import com.gradproject.taskmanager.shared.exception.DuplicateResourceException;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.LabelMapper;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class LabelService {

    private final LabelRepository labelRepository;
    private final TaskLabelRepository taskLabelRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final LabelMapper mapper;

    
    @Transactional
    public LabelResponse createLabel(Long orgId, LabelRequest request, Integer userId) {
        log.debug("Creating label '{}' in organization {} by user {}", request.name(), orgId, userId);

        
        verifyOrganizationMembership(userId, orgId);

        
        if (labelRepository.existsByOrganizationIdAndName(orgId, request.name())) {
            throw new DuplicateResourceException("Label '" + request.name() + "' already exists in this organization");
        }

        
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));

        
        Label label = mapper.fromRequest(request);
        label.setOrganization(org);
        label.setCreatedBy(userId);
        label.setColor(request.getColorOrDefault());

        
        label = labelRepository.save(label);
        log.info("Created label '{}' (id={}) in organization {}", label.getName(), label.getId(), orgId);

        return mapper.toResponse(label);
    }

    
    @Transactional
    public LabelResponse updateLabel(Long labelId, LabelRequest request, Integer userId) {
        log.debug("Updating label {} by user {}", labelId, userId);

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));

        
        verifyOrganizationMembership(userId, label.getOrganization().getId());

        
        if (request.name() != null && !request.name().equals(label.getName())) {
            if (labelRepository.existsByOrganizationIdAndName(label.getOrganization().getId(), request.name())) {
                throw new DuplicateResourceException("Label '" + request.name() + "' already exists in this organization");
            }
        }

        
        mapper.updateFromRequest(request, label);

        label = labelRepository.save(label);
        log.info("Updated label {} in organization {}", labelId, label.getOrganization().getId());

        return mapper.toResponse(label);
    }

    
    @Transactional
    public void deleteLabel(Long labelId, Integer userId) {
        log.debug("Deleting label {} by user {}", labelId, userId);

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));

        
        verifyOrganizationMembership(userId, label.getOrganization().getId());

        
        labelRepository.delete(label);

        log.info("Deleted label '{}' (id={}) from organization {}",
                label.getName(), labelId, label.getOrganization().getId());
    }

    
    @Transactional(readOnly = true)
    public List<LabelResponse> listOrganizationLabels(Long orgId, Integer userId) {
        log.debug("Listing labels for organization {} by user {}", orgId, userId);

        verifyOrganizationMembership(userId, orgId);

        return labelRepository.findByOrganizationIdOrderByNameAsc(orgId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    
    @Transactional(readOnly = true)
    public LabelResponse getLabel(Long labelId, Integer userId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));

        verifyOrganizationMembership(userId, label.getOrganization().getId());

        return mapper.toResponse(label);
    }

    
    @Transactional
    public void addLabelToTask(Long taskId, Long labelId, Integer userId) {
        log.debug("Adding label {} to task {} by user {}", labelId, taskId, userId);

        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!task.getOrganization().getId().equals(label.getOrganization().getId())) {
            throw new BusinessRuleViolationException("Label and task must belong to the same organization");
        }

        
        verifyCanManageTask(user, task.getProject());

        
        if (taskLabelRepository.existsByTaskIdAndLabelId(taskId, labelId)) {
            throw new BusinessRuleViolationException("Label '" + label.getName() + "' is already added to this task");
        }

        
        TaskLabel taskLabel = TaskLabel.builder()
                .task(task)
                .label(label)
                .addedBy(userId)
                .build();

        taskLabelRepository.save(taskLabel);
        log.info("Added label '{}' to task {} by user {}", label.getName(), task.getKey(), userId);
    }

    
    @Transactional
    public void removeLabelFromTask(Long taskId, Long labelId, Integer userId) {
        log.debug("Removing label {} from task {} by user {}", labelId, taskId, userId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        verifyCanManageTask(user, task.getProject());

        
        taskLabelRepository.deleteByTaskIdAndLabelId(taskId, labelId);

        log.info("Removed label from task {} by user {}", task.getKey(), userId);
    }

    
    @Transactional(readOnly = true)
    public List<LabelResponse> getTaskLabels(Long taskId, Integer userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        verifyCanAccessTask(user, task.getProject());

        return taskLabelRepository.findByTaskId(taskId)
                .stream()
                .map(TaskLabel::getLabel)
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    

    
    private void verifyOrganizationMembership(Integer userId, Long orgId) {
        if (!organizationMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new UnauthorizedException("You are not a member of this organization");
        }
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
