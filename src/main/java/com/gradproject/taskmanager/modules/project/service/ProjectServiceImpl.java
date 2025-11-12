package com.gradproject.taskmanager.modules.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationRepository;
import com.gradproject.taskmanager.modules.project.domain.*;
import com.gradproject.taskmanager.modules.project.dto.ProjectCreateRequest;
import com.gradproject.taskmanager.modules.project.dto.ProjectResponse;
import com.gradproject.taskmanager.modules.project.dto.ProjectUpdateRequest;
import com.gradproject.taskmanager.modules.project.repository.ProjectMemberRepository;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final StatusTemplateRepository statusTemplateRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ProjectMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ProjectResponse createProject(Long organizationId, ProjectCreateRequest request, Integer userId) {
        log.info("Creating project with key '{}' in organization {}", request.key(), organizationId);

        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

        
        if (!permissionService.isOrgOwnerOrAdmin(user, organizationId)) {
            throw new UnauthorizedException("Only organization owners and admins can create projects");
        }

        
        String projectKey = request.key().toUpperCase();
        if (projectRepository.existsByOrganizationIdAndKey(organizationId, projectKey)) {
            throw new DuplicateResourceException("Project", "key", projectKey);
        }

        
        Project project = new Project(organization, projectKey, request.name(), request.description(), user);
        project = projectRepository.save(project);
        log.debug("Project entity created with ID {}", project.getId());

        
        if (request.statusTemplateId() != null) {
            applyStatusTemplate(project, request.statusTemplateId());
        } else {
            applyDefaultStatusTemplate(project);
        }

        
        ProjectMember ownerMembership = new ProjectMember(user, project, ProjectRole.PROJECT_OWNER, user);
        projectMemberRepository.save(ownerMembership);
        log.debug("Added user {} as PROJECT_OWNER of project {}", userId, project.getId());

        return mapper.toResponse(project);
    }

    
    private void applyStatusTemplate(Project project, Long templateId) {
        log.info("Applying status template {} to project {}", templateId, project.getId());

        StatusTemplate template = statusTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Status template", templateId));

        List<Map<String, Object>> statusDefinitions = template.getStatuses();
        if (statusDefinitions == null || statusDefinitions.isEmpty()) {
            log.warn("Template {} has no statuses, applying default", templateId);
            applyDefaultStatusTemplate(project);
            return;
        }

        TaskStatus defaultStatus = null;
        int orderIndex = 0;

        for (Map<String, Object> def : statusDefinitions) {
            String name = (String) def.get("name");
            String color = (String) def.get("color");
            String category = (String) def.get("category");
            Integer order = def.get("order") != null ? ((Number) def.get("order")).intValue() : orderIndex;

            TaskStatus status = new TaskStatus(
                    project,
                    name,
                    color,
                    order,
                    StatusCategory.valueOf(category),
                    order == 1  
            );

            status = taskStatusRepository.save(status);
            log.debug("Created status '{}' with order {}", name, order);

            if (status.getIsDefault()) {
                defaultStatus = status;
            }
            orderIndex++;
        }

        
        if (defaultStatus != null) {
            project.setDefaultStatus(defaultStatus);
            projectRepository.save(project);
            log.info("Set default status '{}' for project {}", defaultStatus.getName(), project.getId());
        }
    }

    
    private void applyDefaultStatusTemplate(Project project) {
        log.info("Applying default status template to project {}", project.getId());

        StatusTemplate defaultTemplate = statusTemplateRepository.findByName("Simple Kanban")
                .orElseThrow(() -> new IllegalStateException("Default 'Simple Kanban' template not found in database"));

        applyStatusTemplate(project, defaultTemplate.getId());
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectUpdateRequest request, Integer userId) {
        log.info("Updating project {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canEditProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to edit this project");
        }

        
        project.setName(request.name());
        project.setDescription(request.description());
        project = projectRepository.save(project);

        log.info("Project {} updated successfully", projectId);
        return mapper.toResponse(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, Integer userId) {
        log.info("Deleting project {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canDeleteProject(user, project)) {
            throw new UnauthorizedException("Only project owners can delete projects");
        }

        projectRepository.delete(project);
        log.info("Project {} deleted successfully", projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId, Integer userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have access to this project");
        }

        return mapper.toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> listOrganizationProjects(Long organizationId, Integer userId) {
        log.info("Listing projects for organization {} (user {})", organizationId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        List<Project> allProjects = projectRepository.findByOrganizationId(organizationId);

        
        return allProjects.stream()
                .filter(project -> permissionService.canAccessProject(user, project))
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> listUserProjects(Integer userId) {
        log.info("Listing all projects for user {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<Project> projects = projectRepository.findByUserId(userId);

        return projects.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
}
