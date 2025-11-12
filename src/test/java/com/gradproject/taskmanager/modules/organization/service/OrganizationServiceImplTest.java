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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationMapper mapper;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private OrganizationServiceImpl organizationService;

    private User testUser;
    private Organization testOrganization;
    private OrganizationRequest testRequest;
    private OrganizationResponse testResponse;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1);

        testOrganization = new Organization("Test Org", "test-org", "Test Description", testUser);
        testOrganization.setId(1L);
        testOrganization.setCreatedAt(LocalDateTime.now());

        testRequest = new OrganizationRequest("Test Org", "Test Description");

        testResponse = new OrganizationResponse(
                1L,
                "Test Org",
                "test-org",
                "Test Description",
                1L,
                LocalDateTime.now(),
                "testuser"
        );
    }

    @Test
    void createOrganization_shouldSucceed() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.existsBySlug("test-org")).thenReturn(false);
        when(mapper.toEntity(testRequest)).thenReturn(testOrganization);
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);
        when(organizationRepository.findByIdWithMembers(1L)).thenReturn(Optional.of(testOrganization));
        when(memberRepository.save(any(OrganizationMember.class))).thenReturn(new OrganizationMember());
        when(mapper.toResponse(testOrganization)).thenReturn(testResponse);

        
        OrganizationResponse result = organizationService.createOrganization(testRequest, 1);

        
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Org");
        verify(organizationRepository).save(any(Organization.class));
        verify(memberRepository).save(argThat(member ->
                member.getRole() == OrganizationRole.ORG_OWNER &&
                member.getUser().equals(testUser) &&
                member.getOrganization().equals(testOrganization)
        ));
    }

    @Test
    void createOrganization_shouldThrowWhenUserNotFound() {
        
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> organizationService.createOrganization(testRequest, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void createOrganization_shouldThrowWhenSlugExists() {
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.existsBySlug(anyString())).thenReturn(true);

        
        assertThatThrownBy(() -> organizationService.createOrganization(testRequest, 1))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("slug");
    }

    @Test
    void updateOrganization_shouldSucceedForOwner() {
        
        OrganizationMember ownerMembership = new OrganizationMember();
        ownerMembership.setRole(OrganizationRole.ORG_OWNER);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(memberRepository.findByUserIdAndOrganizationId(1, 1L)).thenReturn(Optional.of(ownerMembership));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);
        when(mapper.toResponse(testOrganization)).thenReturn(testResponse);

        
        OrganizationResponse result = organizationService.updateOrganization(1L, testRequest, 1);

        
        assertThat(result).isNotNull();
        verify(mapper).updateEntityFromRequest(testRequest, testOrganization);
        verify(organizationRepository).save(testOrganization);
    }

    @Test
    void updateOrganization_shouldSucceedForAdmin() {
        
        OrganizationMember adminMembership = new OrganizationMember();
        adminMembership.setRole(OrganizationRole.ORG_ADMIN);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(memberRepository.findByUserIdAndOrganizationId(1, 1L)).thenReturn(Optional.of(adminMembership));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);
        when(mapper.toResponse(testOrganization)).thenReturn(testResponse);

        
        OrganizationResponse result = organizationService.updateOrganization(1L, testRequest, 1);

        
        assertThat(result).isNotNull();
    }

    @Test
    void updateOrganization_shouldThrowForMember() {
        
        OrganizationMember memberMembership = new OrganizationMember();
        memberMembership.setRole(OrganizationRole.ORG_MEMBER);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(memberRepository.findByUserIdAndOrganizationId(1, 1L)).thenReturn(Optional.of(memberMembership));

        
        assertThatThrownBy(() -> organizationService.updateOrganization(1L, testRequest, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("owners and admins");
    }

    @Test
    void updateOrganization_shouldThrowWhenOrganizationNotFound() {
        
        when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> organizationService.updateOrganization(1L, testRequest, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organization");
    }

    @Test
    void deleteOrganization_shouldSucceedForOwner() {
        
        OrganizationMember ownerMembership = new OrganizationMember();
        ownerMembership.setRole(OrganizationRole.ORG_OWNER);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(memberRepository.findByUserIdAndOrganizationId(1, 1L)).thenReturn(Optional.of(ownerMembership));

        
        organizationService.deleteOrganization(1L, 1);

        
        verify(organizationRepository).delete(testOrganization);
    }

    @Test
    void deleteOrganization_shouldThrowForNonOwner() {
        
        OrganizationMember adminMembership = new OrganizationMember();
        adminMembership.setRole(OrganizationRole.ORG_ADMIN);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(memberRepository.findByUserIdAndOrganizationId(1, 1L)).thenReturn(Optional.of(adminMembership));

        
        assertThatThrownBy(() -> organizationService.deleteOrganization(1L, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("owners can delete");
    }

    @Test
    void getOrganization_shouldSucceedForMember() {
        
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(memberRepository.existsByUserIdAndOrganizationId(1, 1L)).thenReturn(true);
        when(mapper.toResponse(testOrganization)).thenReturn(testResponse);

        
        OrganizationResponse result = organizationService.getOrganization(1L, 1);

        
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Org");
    }

    @Test
    void getOrganization_shouldThrowForNonMember() {
        
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(memberRepository.existsByUserIdAndOrganizationId(1, 1L)).thenReturn(false);

        
        assertThatThrownBy(() -> organizationService.getOrganization(1L, 1))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not a member");
    }

    @Test
    void getOrganizationBySlug_shouldSucceed() {
        
        when(organizationRepository.findBySlug("test-org")).thenReturn(Optional.of(testOrganization));
        when(memberRepository.existsByUserIdAndOrganizationId(1, 1L)).thenReturn(true);
        when(mapper.toResponse(testOrganization)).thenReturn(testResponse);

        
        OrganizationResponse result = organizationService.getOrganizationBySlug("test-org", 1);

        
        assertThat(result).isNotNull();
        assertThat(result.slug()).isEqualTo("test-org");
    }

    @Test
    void getOrganizationBySlug_shouldThrowWhenNotFound() {
        
        when(organizationRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> organizationService.getOrganizationBySlug("nonexistent", 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listMyOrganizations_shouldReturnUserOrganizations() {
        
        Organization org1 = new Organization("Org 1", "org-1", "Desc 1", testUser);
        Organization org2 = new Organization("Org 2", "org-2", "Desc 2", testUser);
        List<Organization> organizations = List.of(org1, org2);

        when(organizationRepository.findByUserId(1)).thenReturn(organizations);
        when(mapper.toResponse(any(Organization.class))).thenReturn(testResponse);

        
        List<OrganizationResponse> result = organizationService.listMyOrganizations(1);

        
        assertThat(result).hasSize(2);
        verify(mapper, times(2)).toResponse(any(Organization.class));
    }
}
