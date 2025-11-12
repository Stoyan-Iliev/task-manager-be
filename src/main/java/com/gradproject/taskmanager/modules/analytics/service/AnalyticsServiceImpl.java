package com.gradproject.taskmanager.modules.analytics.service;

import com.gradproject.taskmanager.modules.analytics.dto.*;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.StatusCategory;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.release.domain.enums.ReleaseStatus;
import com.gradproject.taskmanager.modules.release.repository.ReleaseRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ReleaseRepository releaseRepository;
    private final PermissionService permissionService;

    @Override
    @Transactional(readOnly = true)
    public ProjectMetricsResponse getProjectMetrics(Long projectId, Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to view metrics for this project");
        }

        
        List<Task> tasks = taskRepository.findByProjectWithFilters(projectId, null, null, null);

        long totalTasks = tasks.size();
        long completedTasks = tasks.stream()
            .filter(t -> t.getStatus() != null && StatusCategory.DONE == t.getStatus().getCategory())
            .count();
        long inProgressTasks = tasks.stream()
            .filter(t -> t.getStatus() != null && StatusCategory.IN_PROGRESS == t.getStatus().getCategory())
            .count();
        long todoTasks = tasks.stream()
            .filter(t -> t.getStatus() != null && StatusCategory.TODO == t.getStatus().getCategory())
            .count();
        long overdueTasks = taskRepository.findOverdueTasks(projectId, LocalDate.now()).size();

        double completionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0.0;
        long totalMembers = project.getMembers() != null ? project.getMembers().size() : 0L;
        long activeReleases = releaseRepository.findByProjectIdAndStatus(projectId, ReleaseStatus.IN_PROGRESS).size();

        return new ProjectMetricsResponse(
            projectId,
            project.getName(),
            totalTasks,
            completedTasks,
            inProgressTasks,
            todoTasks,
            overdueTasks,
            completionRate,
            totalMembers,
            activeReleases
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMetricsResponse> getOrganizationProjectsMetrics(Long organizationId, Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        List<Project> projects = projectRepository.findByOrganizationId(organizationId);

        return projects.stream()
            .filter(project -> permissionService.canAccessProject(user, project))
            .map(project -> getProjectMetrics(project.getId(), userId))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationMetricsResponse getOrganizationMetrics(Long organizationId, Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        List<Project> projects = projectRepository.findByOrganizationId(organizationId);
        List<Project> accessibleProjects = projects.stream()
            .filter(project -> permissionService.canAccessProject(user, project))
            .collect(Collectors.toList());

        long totalProjects = accessibleProjects.size();
        long activeProjects = totalProjects; 

        
        long totalTasks = 0;
        long completedTasks = 0;

        for (Project project : accessibleProjects) {
            List<Task> tasks = taskRepository.findByProjectWithFilters(project.getId(), null, null, null);
            totalTasks += tasks.size();
            completedTasks += tasks.stream()
                .filter(t -> t.getStatus() != null && StatusCategory.DONE == t.getStatus().getCategory())
                .count();
        }

        double overallCompletionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0.0;
        long totalMembers = organization.getMembers() != null ? organization.getMembers().size() : 0L;

        return new OrganizationMetricsResponse(
            organizationId,
            organization.getName(),
            totalProjects,
            totalTasks,
            completedTasks,
            totalMembers,
            activeProjects,
            overallCompletionRate
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserActivityResponse getUserActivity(Integer userId, Long organizationId, Integer requestingUserId) {
        User requestingUser = userRepository.findById(requestingUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestingUserId));

        User targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        
        List<Task> assignedTasks = taskRepository.findMyOpenTasks(userId, organizationId);
        long tasksAssigned = assignedTasks.size();
        long tasksCompleted = assignedTasks.stream()
            .filter(t -> t.getStatus() != null && StatusCategory.DONE == t.getStatus().getCategory())
            .count();

        
        
        long tasksCreated = 0; 
        long commentsCreated = 0; 
        long tasksWatching = 0; 

        return new UserActivityResponse(
            userId,
            targetUser.getUsername(),
            tasksAssigned,
            tasksCompleted,
            tasksCreated,
            commentsCreated,
            tasksWatching
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TaskStatusDistributionResponse getTaskStatusDistribution(Long projectId, Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!permissionService.canAccessProject(user, project)) {
            throw new UnauthorizedException("You don't have permission to view metrics for this project");
        }

        List<Task> tasks = taskRepository.findByProjectWithFilters(projectId, null, null, null);

        Map<String, Long> distribution = new HashMap<>();
        for (Task task : tasks) {
            if (task.getStatus() != null) {
                String statusName = task.getStatus().getName();
                distribution.put(statusName, distribution.getOrDefault(statusName, 0L) + 1);
            }
        }

        return new TaskStatusDistributionResponse(
            projectId,
            project.getName(),
            distribution
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TimeRangeMetricsResponse getTimeRangeMetrics(Long organizationId, LocalDate startDate, LocalDate endDate, Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        
        
        long tasksCreated = 0;
        long tasksCompleted = 0;
        long commentsCreated = 0;
        long activitiesLogged = 0;

        return new TimeRangeMetricsResponse(
            startDate,
            endDate,
            tasksCreated,
            tasksCompleted,
            commentsCreated,
            activitiesLogged
        );
    }
}
