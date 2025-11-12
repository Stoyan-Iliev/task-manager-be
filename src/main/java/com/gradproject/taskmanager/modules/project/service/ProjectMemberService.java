package com.gradproject.taskmanager.modules.project.service;

import com.gradproject.taskmanager.modules.project.domain.ProjectRole;
import com.gradproject.taskmanager.modules.project.dto.AddProjectMemberRequest;
import com.gradproject.taskmanager.modules.project.dto.ProjectMemberResponse;

import java.util.List;


public interface ProjectMemberService {

    
    ProjectMemberResponse addMember(Long projectId, AddProjectMemberRequest request, Integer addedBy);

    
    void removeMember(Long projectId, Integer userId, Integer removedBy);

    
    ProjectMemberResponse updateMemberRole(Long projectId, Integer userId, ProjectRole newRole, Integer updatedBy);

    
    List<ProjectMemberResponse> listProjectMembers(Long projectId, Integer requesterId);
}
