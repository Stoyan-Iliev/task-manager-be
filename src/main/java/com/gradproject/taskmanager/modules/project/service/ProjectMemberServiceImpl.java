package com.gradproject.taskmanager.modules.project.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.ProjectMember;
import com.gradproject.taskmanager.modules.project.domain.ProjectRole;
import com.gradproject.taskmanager.modules.project.dto.AddProjectMemberRequest;
import com.gradproject.taskmanager.modules.project.dto.ProjectMemberResponse;
import com.gradproject.taskmanager.modules.project.repository.ProjectMemberRepository;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.shared.exception.BusinessRuleViolationException;
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
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ProjectMapper mapper;

    @Override
    @Transactional
    public ProjectMemberResponse addMember(Long projectId, AddProjectMemberRequest request, Integer addedBy) {
        log.info("Adding user {} to project {} with role {}", request.userId(), projectId, request.role());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User adder = userRepository.findById(addedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", addedBy));

        User userToAdd = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

        
        if (!permissionService.canManageMembers(adder, project)) {
            throw new UnauthorizedException("You don't have permission to manage project members");
        }

        
        if (projectMemberRepository.existsByUserIdAndProjectId(request.userId(), projectId)) {
            throw new DuplicateResourceException("Project member", "userId", request.userId().toString());
        }

        
        ProjectMember member = new ProjectMember(userToAdd, project, request.role(), adder);
        member = projectMemberRepository.save(member);

        log.info("Successfully added user {} to project {}", request.userId(), projectId);
        return mapper.toMemberResponse(member);
    }

    @Override
    @Transactional
    public void removeMember(Long projectId, Integer userId, Integer removedBy) {
        log.info("Removing user {} from project {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User remover = userRepository.findById(removedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", removedBy));

        
        if (!permissionService.canManageMembers(remover, project)) {
            throw new UnauthorizedException("You don't have permission to manage project members");
        }

        ProjectMember member = projectMemberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found"));

        
        if (member.getRole() == ProjectRole.PROJECT_OWNER) {
            long ownerCount = projectMemberRepository.countByProjectIdAndRole(projectId, ProjectRole.PROJECT_OWNER);
            if (ownerCount <= 1) {
                throw new BusinessRuleViolationException("Cannot remove the last project owner");
            }
        }

        projectMemberRepository.delete(member);
        log.info("Successfully removed user {} from project {}", userId, projectId);
    }

    @Override
    @Transactional
    public ProjectMemberResponse updateMemberRole(Long projectId, Integer userId, ProjectRole newRole, Integer updatedBy) {
        log.info("Updating role for user {} in project {} to {}", userId, projectId, newRole);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User updater = userRepository.findById(updatedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", updatedBy));

        
        if (!permissionService.canManageMembers(updater, project)) {
            throw new UnauthorizedException("You don't have permission to manage project members");
        }

        ProjectMember member = projectMemberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found"));

        
        if (member.getRole() == ProjectRole.PROJECT_OWNER && newRole != ProjectRole.PROJECT_OWNER) {
            long ownerCount = projectMemberRepository.countByProjectIdAndRole(projectId, ProjectRole.PROJECT_OWNER);
            if (ownerCount <= 1) {
                throw new BusinessRuleViolationException("Cannot change the role of the last project owner");
            }
        }

        member.setRole(newRole);
        member = projectMemberRepository.save(member);

        log.info("Successfully updated role for user {} in project {}", userId, projectId);
        return mapper.toMemberResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listProjectMembers(Long projectId, Integer requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", requesterId));

        
        if (!permissionService.canAccessProject(requester, project)) {
            throw new UnauthorizedException("You don't have access to this project");
        }

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        return members.stream()
                .map(mapper::toMemberResponse)
                .collect(Collectors.toList());
    }
}
