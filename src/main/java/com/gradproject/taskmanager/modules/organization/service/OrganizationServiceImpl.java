package com.gradproject.taskmanager.modules.organization.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationMember;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationRole;
import com.gradproject.taskmanager.modules.organization.dto.OrganizationRequest;
import com.gradproject.taskmanager.modules.organization.dto.OrganizationResponse;
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
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final OrganizationMapper mapper;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request, Integer userId) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        String slug = request.generateSlug();
        if (organizationRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Organization", "slug", slug);
        }

        
        Organization organization = mapper.toEntity(request);
        organization.setSlug(slug);
        organization.setCreatedBy(user);
        organization = organizationRepository.save(organization);

        final Long orgId = organization.getId();

        
        OrganizationMember ownerMembership = new OrganizationMember(
                user,
                organization,
                OrganizationRole.ORG_OWNER,
                user
        );
        memberRepository.save(ownerMembership);

        
        
        if (entityManager != null) {
            entityManager.flush();
            entityManager.clear();
        }

        
        Organization reloadedOrg = organizationRepository.findByIdWithMembers(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));

        return mapper.toResponse(reloadedOrg);
    }

    @Override
    @Transactional
    public OrganizationResponse updateOrganization(Long organizationId, OrganizationRequest request, Integer userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

        
        OrganizationRole userRole = getUserRole(userId, organizationId);
        if (userRole != OrganizationRole.ORG_OWNER && userRole != OrganizationRole.ORG_ADMIN) {
            throw new UnauthorizedException("Only organization owners and admins can update organizations");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        mapper.updateEntityFromRequest(request, organization);
        organization.setUpdatedBy(user);
        organization = organizationRepository.save(organization);

        return mapper.toResponse(organization);
    }

    @Override
    @Transactional
    public void deleteOrganization(Long organizationId, Integer userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

        
        OrganizationRole userRole = getUserRole(userId, organizationId);
        if (userRole != OrganizationRole.ORG_OWNER) {
            throw new UnauthorizedException("Only organization owners can delete organizations");
        }

        organizationRepository.delete(organization);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(Long organizationId, Integer userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

        
        if (!memberRepository.existsByUserIdAndOrganizationId(userId, organizationId)) {
            throw new UnauthorizedException("You are not a member of this organization");
        }

        return mapper.toResponse(organization);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationBySlug(String slug, Integer userId) {
        Organization organization = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization with slug '" + slug + "' not found"));

        
        if (!memberRepository.existsByUserIdAndOrganizationId(userId, organization.getId())) {
            throw new UnauthorizedException("You are not a member of this organization");
        }

        return mapper.toResponse(organization);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> listMyOrganizations(Integer userId) {
        List<Organization> organizations = organizationRepository.findByUserId(userId);
        return organizations.stream()
                .map(mapper::toResponse)
                .toList();
    }

    
    private OrganizationRole getUserRole(Integer userId, Long organizationId) {
        return memberRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .map(OrganizationMember::getRole)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this organization"));
    }
}
