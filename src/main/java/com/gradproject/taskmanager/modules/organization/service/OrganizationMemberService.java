package com.gradproject.taskmanager.modules.organization.service;

import com.gradproject.taskmanager.modules.organization.domain.OrganizationRole;
import com.gradproject.taskmanager.modules.organization.dto.MemberResponse;

import java.util.List;


public interface OrganizationMemberService {

    
    MemberResponse addMember(Long organizationId, String email, OrganizationRole role, Integer invitedBy);

    
    void removeMember(Long organizationId, Integer userId, Integer removedBy);

    
    MemberResponse updateMemberRole(Long organizationId, Integer userId, OrganizationRole newRole, Integer updatedBy);

    
    List<MemberResponse> listMembers(Long organizationId, Integer requestingUser);

    
    MemberResponse getMember(Long organizationId, Integer userId, Integer requestingUser);
}
