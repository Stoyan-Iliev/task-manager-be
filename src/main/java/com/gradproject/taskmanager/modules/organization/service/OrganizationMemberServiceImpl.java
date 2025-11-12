package com.gradproject.taskmanager.modules.organization.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationMember;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationRole;
import com.gradproject.taskmanager.modules.organization.dto.MemberResponse;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationMemberRepository;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationRepository;
import com.gradproject.taskmanager.shared.exception.DuplicateResourceException;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.OrganizationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class OrganizationMemberServiceImpl implements OrganizationMemberService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final OrganizationMapper mapper;

    @Override
    @Transactional
    public MemberResponse addMember(Long organizationId, String email, OrganizationRole role, Integer invitedBy) {
        
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

        
        OrganizationRole inviterRole = getUserRole(invitedBy, organizationId);
        if (inviterRole != OrganizationRole.ORG_OWNER && inviterRole != OrganizationRole.ORG_ADMIN) {
            throw new UnauthorizedException("Only organization owners and admins can add members");
        }

        
        User userToAdd = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + email + "' not found"));

        
        if (memberRepository.existsByUserIdAndOrganizationId(userToAdd.getId(), organizationId)) {
            throw new DuplicateResourceException("User is already a member of this organization");
        }

        User inviter = userRepository.findById(invitedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", invitedBy));

        
        OrganizationMember member = new OrganizationMember(
                userToAdd,
                organization,
                role,
                inviter
        );
        member = memberRepository.save(member);

        return mapper.toMemberResponse(member);
    }

    @Override
    @Transactional
    public void removeMember(Long organizationId, Integer userId, Integer removedBy) {
        
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", organizationId);
        }

        
        OrganizationRole removerRole = getUserRole(removedBy, organizationId);
        if (removerRole != OrganizationRole.ORG_OWNER && removerRole != OrganizationRole.ORG_ADMIN) {
            throw new UnauthorizedException("Only organization owners and admins can remove members");
        }

        
        OrganizationMember member = memberRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this organization"));

        
        if (member.getRole() == OrganizationRole.ORG_OWNER) {
            long ownerCount = memberRepository.findByOrganizationIdAndRole(organizationId, OrganizationRole.ORG_OWNER).size();
            if (ownerCount <= 1) {
                throw new IllegalStateException("Cannot remove the last owner. Transfer ownership first.");
            }
        }

        memberRepository.delete(member);
    }

    @Override
    @Transactional
    public MemberResponse updateMemberRole(Long organizationId, Integer userId, OrganizationRole newRole, Integer updatedBy) {
        
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", organizationId);
        }

        
        OrganizationRole updaterRole = getUserRole(updatedBy, organizationId);
        if (updaterRole != OrganizationRole.ORG_OWNER) {
            throw new UnauthorizedException("Only organization owners can change member roles");
        }

        
        OrganizationMember member = memberRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this organization"));

        
        if (member.getRole() == OrganizationRole.ORG_OWNER && newRole != OrganizationRole.ORG_OWNER) {
            long ownerCount = memberRepository.findByOrganizationIdAndRole(organizationId, OrganizationRole.ORG_OWNER).size();
            if (ownerCount <= 1) {
                throw new IllegalStateException("Cannot change the last owner's role. Assign another owner first.");
            }
        }

        
        member.setRole(newRole);
        member = memberRepository.save(member);

        return mapper.toMemberResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long organizationId, Integer requestingUser) {
        
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", organizationId);
        }

        
        if (!memberRepository.existsByUserIdAndOrganizationId(requestingUser, organizationId)) {
            throw new UnauthorizedException("You are not a member of this organization");
        }

        List<OrganizationMember> members = memberRepository.findByOrganizationId(organizationId);
        return members.stream()
                .map(mapper::toMemberResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMember(Long organizationId, Integer userId, Integer requestingUser) {
        
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", organizationId);
        }

        
        if (!memberRepository.existsByUserIdAndOrganizationId(requestingUser, organizationId)) {
            throw new UnauthorizedException("You are not a member of this organization");
        }

        OrganizationMember member = memberRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this organization"));

        return mapper.toMemberResponse(member);
    }

    
    private OrganizationRole getUserRole(Integer userId, Long organizationId) {
        return memberRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .map(OrganizationMember::getRole)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this organization"));
    }
}
