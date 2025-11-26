package com.gradproject.taskmanager.modules.project.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.StatusCategory;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.project.dto.ReorderStatusesRequest;
import com.gradproject.taskmanager.modules.project.dto.StatusTemplateResponse;
import com.gradproject.taskmanager.modules.project.dto.TaskStatusRequest;
import com.gradproject.taskmanager.modules.project.dto.TaskStatusResponse;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.project.repository.StatusTemplateRepository;
import com.gradproject.taskmanager.modules.project.repository.TaskStatusRepository;
import com.gradproject.taskmanager.shared.exception.DuplicateResourceException;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.ProjectMapper;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStatusServiceImpl implements TaskStatusService {

    private final TaskStatusRepository taskStatusRepository;
    private final ProjectRepository projectRepository;
    private final StatusTemplateRepository statusTemplateRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ProjectMapper mapper;

    @Override
    @Transactional
    public TaskStatusResponse createStatus(Long projectId, TaskStatusRequest request, Integer userId) {
        log.info("Creating status '{}' in project {}", request.name(), projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canEditProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to manage project statuses");
        }

        
        if (taskStatusRepository.existsByProjectIdAndName(projectId, request.name())) {
            throw new DuplicateResourceException("Status", "name", request.name());
        }

        
        Integer maxOrderIndex = taskStatusRepository.findMaxOrderIndexByProjectId(projectId);
        Integer orderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;

        
        TaskStatus status = new TaskStatus(project, request.name(), request.color(), orderIndex, request.category(), false);
        status = taskStatusRepository.save(status);

        log.info("Successfully created status '{}' with order {}", request.name(), orderIndex);
        return mapper.toStatusResponse(status);
    }

    @Override
    @Transactional
    public TaskStatusResponse updateStatus(Long statusId, TaskStatusRequest request, Integer userId) {
        log.info("Updating status {}", statusId);

        TaskStatus status = taskStatusRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status", statusId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canEditProject(user, status.getProject())) {
            throw new UnauthorizedException("You don't have permission to manage project statuses");
        }

        
        if (taskStatusRepository.existsByProjectIdAndNameExcludingId(
                status.getProject().getId(), request.name(), statusId)) {
            throw new DuplicateResourceException("Status", "name", request.name());
        }

        
        status.setName(request.name());
        status.setColor(request.color());
        status.setCategory(request.category());
        status = taskStatusRepository.save(status);

        log.info("Successfully updated status {}", statusId);
        return mapper.toStatusResponse(status);
    }

    @Override
    @Transactional
    public void deleteStatus(Long statusId, Integer userId) {
        log.info("Deleting status {}", statusId);

        TaskStatus status = taskStatusRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status", statusId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canEditProject(user, status.getProject())) {
            throw new UnauthorizedException("You don't have permission to manage project statuses");
        }

        
        

        taskStatusRepository.delete(status);
        log.info("Successfully deleted status {}", statusId);
    }

    @Override
    @Transactional
    public List<TaskStatusResponse> reorderStatuses(Long projectId, ReorderStatusesRequest request, Integer userId) {
        log.info("Reordering statuses for project {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canEditProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to manage project statuses");
        }

        
        List<Long> statusIds = request.statusIds();
        for (int i = 0; i < statusIds.size(); i++) {
            Long statusId = statusIds.get(i);
            TaskStatus status = taskStatusRepository.findById(statusId)
                    .orElseThrow(() -> new ResourceNotFoundException("Status", statusId));

            
            if (!status.getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Status " + statusId + " does not belong to project " + projectId);
            }

            status.setOrderIndex(i);
            taskStatusRepository.save(status);
        }

        log.info("Successfully reordered statuses for project {}", projectId);

        
        return taskStatusRepository.findByProjectIdOrderByOrderIndexAsc(projectId)
                .stream()
                .map(mapper::toStatusResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskStatusResponse> listProjectStatuses(Long projectId, Integer requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", requesterId));

        
        if (!permissionService.canAccessProject(requester, project)) {
            throw new UnauthorizedException("You don't have access to this project");
        }

        List<TaskStatus> statuses = taskStatusRepository.findByProjectIdOrderByOrderIndexAsc(projectId);
        return statuses.stream()
                .map(mapper::toStatusResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusTemplateResponse> getStatusTemplates() {
        return statusTemplateRepository.findAllOrderByName()
                .stream()
                .map(mapper::toTemplateResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<TaskStatusResponse> applyStatusTemplate(Long projectId, String templateId, Integer userId) {
        log.info("Applying status template '{}' to project {}", templateId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Verify user can edit project
        if (!permissionService.canEditProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to manage project statuses");
        }

        // Get template definition
        List<Map<String, String>> templateStatuses = getTemplateDefinition(templateId);
        if (templateStatuses == null) {
            throw new ResourceNotFoundException("Status template", templateId);
        }

        // Delete existing statuses
        List<TaskStatus> existingStatuses = taskStatusRepository.findByProjectIdOrderByOrderIndexAsc(projectId);
        taskStatusRepository.deleteAll(existingStatuses);

        // Create new statuses from template
        List<TaskStatus> newStatuses = new ArrayList<>();
        for (int i = 0; i < templateStatuses.size(); i++) {
            Map<String, String> statusDef = templateStatuses.get(i);
            StatusCategory category = StatusCategory.valueOf(statusDef.get("category"));
            TaskStatus status = new TaskStatus(
                    project,
                    statusDef.get("name"),
                    statusDef.get("color"),
                    i,
                    category,
                    false
            );
            newStatuses.add(taskStatusRepository.save(status));
        }

        log.info("Successfully applied template '{}' with {} statuses to project {}",
                templateId, newStatuses.size(), projectId);

        return newStatuses.stream()
                .map(mapper::toStatusResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns predefined status template definitions matching the frontend.
     */
    private List<Map<String, String>> getTemplateDefinition(String templateId) {
        return switch (templateId) {
            case "BASIC" -> List.of(
                    Map.of("name", "To Do", "category", "TODO", "color", "#9e9e9e"),
                    Map.of("name", "In Progress", "category", "IN_PROGRESS", "color", "#2196f3"),
                    Map.of("name", "Done", "category", "DONE", "color", "#4caf50")
            );
            case "SCRUM" -> List.of(
                    Map.of("name", "Backlog", "category", "TODO", "color", "#9e9e9e"),
                    Map.of("name", "Selected for Development", "category", "TODO", "color", "#2196f3"),
                    Map.of("name", "In Progress", "category", "IN_PROGRESS", "color", "#2196f3"),
                    Map.of("name", "In Review", "category", "IN_PROGRESS", "color", "#9c27b0"),
                    Map.of("name", "Done", "category", "DONE", "color", "#4caf50")
            );
            case "KANBAN" -> List.of(
                    Map.of("name", "To Do", "category", "TODO", "color", "#9e9e9e"),
                    Map.of("name", "In Progress", "category", "IN_PROGRESS", "color", "#2196f3"),
                    Map.of("name", "Testing", "category", "IN_PROGRESS", "color", "#ff9800"),
                    Map.of("name", "Done", "category", "DONE", "color", "#4caf50")
            );
            case "SOFTWARE_DEVELOPMENT" -> List.of(
                    Map.of("name", "Backlog", "category", "TODO", "color", "#9e9e9e"),
                    Map.of("name", "Design", "category", "IN_PROGRESS", "color", "#9c27b0"),
                    Map.of("name", "Development", "category", "IN_PROGRESS", "color", "#2196f3"),
                    Map.of("name", "Code Review", "category", "IN_PROGRESS", "color", "#ff9800"),
                    Map.of("name", "Testing", "category", "IN_PROGRESS", "color", "#ff9800"),
                    Map.of("name", "Ready for Deploy", "category", "IN_PROGRESS", "color", "#4caf50"),
                    Map.of("name", "Done", "category", "DONE", "color", "#4caf50")
            );
            default -> null;
        };
    }
}
