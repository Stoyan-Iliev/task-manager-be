package com.gradproject.taskmanager.shared.security;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationMember;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationRole;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationMemberRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.ProjectMember;
import com.gradproject.taskmanager.modules.project.domain.ProjectRole;
import com.gradproject.taskmanager.modules.project.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProjectMemberRepository projectMemberRepository;

    
    public ProjectRole getEffectiveProjectRole(User user, Project project) {
        
        Optional<OrganizationMember> orgMemberOpt = organizationMemberRepository
                .findByUserIdAndOrganizationId(user.getId(), project.getOrganization().getId());

        if (orgMemberOpt.isEmpty()) {
            log.debug("User {} is not a member of organization {}", user.getId(), project.getOrganization().getId());
            return null; 
        }

        OrganizationMember orgMember = orgMemberOpt.get();
        OrganizationRole orgRole = orgMember.getRole();

        
        if (orgRole == OrganizationRole.ORG_OWNER || orgRole == OrganizationRole.ORG_ADMIN) {
            
            Optional<ProjectMember> projectMemberOpt = projectMemberRepository
                    .findByUserIdAndProjectId(user.getId(), project.getId());

            if (projectMemberOpt.isPresent()) {
                ProjectRole explicitRole = projectMemberOpt.get().getRole();
                log.debug("User {} has explicit project role {} on project {} (overriding org-level {})",
                        user.getId(), explicitRole, project.getId(), orgRole);
                return explicitRole;
            }

            
            log.debug("User {} inherits PROJECT_ADMIN from org role {} on project {}",
                    user.getId(), orgRole, project.getId());
            return ProjectRole.PROJECT_ADMIN;
        }

        
        Optional<ProjectMember> projectMemberOpt = projectMemberRepository
                .findByUserIdAndProjectId(user.getId(), project.getId());

        if (projectMemberOpt.isPresent()) {
            ProjectRole role = projectMemberOpt.get().getRole();
            log.debug("User {} has explicit project role {} on project {}",
                    user.getId(), role, project.getId());
            return role;
        }

        log.debug("User {} is ORG_MEMBER but not explicitly added to project {}", user.getId(), project.getId());
        return null; 
    }

    
    public boolean canAccessProject(User user, Project project) {
        ProjectRole role = getEffectiveProjectRole(user, project);
        return role != null; 
    }

    
    public boolean canEditProject(User user, Project project) {
        ProjectRole role = getEffectiveProjectRole(user, project);
        return role == ProjectRole.PROJECT_OWNER || role == ProjectRole.PROJECT_ADMIN;
    }

    
    public boolean canDeleteProject(User user, Project project) {
        ProjectRole role = getEffectiveProjectRole(user, project);
        return role == ProjectRole.PROJECT_OWNER;
    }

    
    public boolean canManageMembers(User user, Project project) {
        ProjectRole role = getEffectiveProjectRole(user, project);
        return role == ProjectRole.PROJECT_OWNER || role == ProjectRole.PROJECT_ADMIN;
    }

    
    public boolean canManageTasks(User user, Project project) {
        ProjectRole role = getEffectiveProjectRole(user, project);
        return role == ProjectRole.PROJECT_OWNER
                || role == ProjectRole.PROJECT_ADMIN
                || role == ProjectRole.PROJECT_MEMBER;
    }

    
    public boolean isOrgOwnerOrAdmin(User user, Long organizationId) {
        return organizationMemberRepository
                .findByUserIdAndOrganizationId(user.getId(), organizationId)
                .map(member -> member.getRole() == OrganizationRole.ORG_OWNER
                        || member.getRole() == OrganizationRole.ORG_ADMIN)
                .orElse(false);
    }
}
