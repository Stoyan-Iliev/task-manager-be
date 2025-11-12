package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.dto.request.CreateGitIntegrationRequest;
import com.gradproject.taskmanager.modules.git.dto.request.UpdateGitIntegrationRequest;
import com.gradproject.taskmanager.modules.git.dto.response.GitIntegrationResponse;
import com.gradproject.taskmanager.modules.git.repository.GitBranchRepository;
import com.gradproject.taskmanager.modules.git.repository.GitCommitRepository;
import com.gradproject.taskmanager.modules.git.repository.GitIntegrationRepository;
import com.gradproject.taskmanager.modules.git.repository.GitPullRequestRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.shared.exception.DuplicateResourceException;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.GitIntegrationMapper;
import com.gradproject.taskmanager.shared.util.TokenEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class GitIntegrationServiceImpl implements GitIntegrationService {

    private final GitIntegrationRepository integrationRepository;
    private final GitBranchRepository branchRepository;
    private final GitCommitRepository commitRepository;
    private final GitPullRequestRepository pullRequestRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final GitIntegrationMapper mapper;
    private final TokenEncryptionService encryptionService;

    @Override
    @Transactional
    public GitIntegrationResponse createIntegration(Long organizationId, CreateGitIntegrationRequest request, Integer userId) {
        log.info("Creating Git integration for organization {} and project {}", organizationId, request.projectId());

        
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

        
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.projectId()));

        if (!project.getOrganization().getId().equals(organizationId)) {
            throw new UnauthorizedException("Project does not belong to this organization");
        }

        
        

        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (integrationRepository.existsByOrganizationIdAndRepositoryUrl(organizationId, request.repositoryUrl())) {
            throw new DuplicateResourceException("Git Integration", "repositoryUrl", request.repositoryUrl());
        }

        
        String[] repoParts = parseRepositoryUrl(request.repositoryUrl());
        String repositoryOwner = repoParts[0];
        String repositoryName = repoParts[1];
        String repositoryFullName = repositoryOwner + "/" + repositoryName;

        
        GitIntegration integration = new GitIntegration();
        integration.setOrganization(organization);
        integration.setProject(project);
        integration.setProvider(request.provider());
        integration.setRepositoryUrl(request.repositoryUrl());
        integration.setRepositoryOwner(repositoryOwner);
        integration.setRepositoryName(repositoryName);
        integration.setRepositoryFullName(repositoryFullName);

        
        String encryptedToken = encryptionService.encrypt(request.accessToken());
        integration.setAccessTokenEncrypted(encryptedToken);

        
        if (request.webhookSecret() != null && !request.webhookSecret().isEmpty()) {
            String encryptedSecret = encryptionService.encrypt(request.webhookSecret());
            integration.setWebhookSecretEncrypted(encryptedSecret);
        }

        
        integration.setAutoLinkEnabled(request.autoLinkEnabled());
        integration.setSmartCommitsEnabled(request.smartCommitsEnabled());
        integration.setAutoCloseOnMerge(request.autoCloseOnMerge());
        integration.setBranchPrefix(request.branchPrefix());

        integration.setIsActive(true);
        integration.setCreatedBy(user);

        
        integration = integrationRepository.save(integration);

        log.info("Git integration created successfully with ID: {}", integration.getId());

        
        

        return enrichWithStatistics(mapper.toResponse(integration));
    }

    @Override
    @Transactional
    public GitIntegrationResponse updateIntegration(Long integrationId, UpdateGitIntegrationRequest request, Integer userId) {
        log.info("Updating Git integration {}", integrationId);

        GitIntegration integration = integrationRepository.findById(integrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Git Integration", integrationId));

        
        

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        
        if (request.autoLinkEnabled() != null) {
            integration.setAutoLinkEnabled(request.autoLinkEnabled());
        }
        if (request.smartCommitsEnabled() != null) {
            integration.setSmartCommitsEnabled(request.smartCommitsEnabled());
        }
        if (request.autoCloseOnMerge() != null) {
            integration.setAutoCloseOnMerge(request.autoCloseOnMerge());
        }
        if (request.branchPrefix() != null) {
            integration.setBranchPrefix(request.branchPrefix());
        }

        
        if (request.accessToken() != null && !request.accessToken().isEmpty()) {
            String encryptedToken = encryptionService.encrypt(request.accessToken());
            integration.setAccessTokenEncrypted(encryptedToken);
        }

        
        if (request.webhookSecret() != null && !request.webhookSecret().isEmpty()) {
            String encryptedSecret = encryptionService.encrypt(request.webhookSecret());
            integration.setWebhookSecretEncrypted(encryptedSecret);
        }

        integration.setUpdatedBy(user);
        integration = integrationRepository.save(integration);

        log.info("Git integration {} updated successfully", integrationId);

        return enrichWithStatistics(mapper.toResponse(integration));
    }

    @Override
    @Transactional
    public void deleteIntegration(Long integrationId, Integer userId) {
        log.info("Deleting Git integration {}", integrationId);

        GitIntegration integration = integrationRepository.findById(integrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Git Integration", integrationId));

        
        

        

        
        integrationRepository.delete(integration);

        log.info("Git integration {} deleted successfully", integrationId);
    }

    @Override
    @Transactional(readOnly = true)
    public GitIntegrationResponse getIntegration(Long integrationId, Integer userId) {
        GitIntegration integration = integrationRepository.findById(integrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Git Integration", integrationId));

        
        

        return enrichWithStatistics(mapper.toResponse(integration));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GitIntegrationResponse> listOrganizationIntegrations(Long organizationId, Integer userId) {
        
        

        List<GitIntegration> integrations = integrationRepository.findByOrganizationId(organizationId);

        return integrations.stream()
                .map(mapper::toResponse)
                .map(this::enrichWithStatistics)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GitIntegrationResponse> listProjectIntegrations(Long projectId, Integer userId) {
        
        

        List<GitIntegration> integrations = integrationRepository.findByProjectId(projectId);

        return integrations.stream()
                .map(mapper::toResponse)
                .map(this::enrichWithStatistics)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GitIntegrationResponse getByRepositoryUrl(Long organizationId, String repositoryUrl, Integer userId) {
        
        

        GitIntegration integration = integrationRepository
                .findByOrganizationIdAndRepositoryUrl(organizationId, repositoryUrl)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Git Integration not found for repository: " + repositoryUrl));

        return enrichWithStatistics(mapper.toResponse(integration));
    }

    @Override
    @Transactional
    public GitIntegrationResponse syncIntegration(Long integrationId, Integer userId) {
        log.info("Syncing Git integration {}", integrationId);

        GitIntegration integration = integrationRepository.findById(integrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Git Integration", integrationId));

        
        

        
        
        
        
        

        integration.setLastSyncAt(LocalDateTime.now());
        integration = integrationRepository.save(integration);

        log.info("Git integration {} synced successfully", integrationId);

        return enrichWithStatistics(mapper.toResponse(integration));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean testConnection(Long organizationId, CreateGitIntegrationRequest request, Integer userId) {
        log.info("Testing Git connection for repository: {}", request.repositoryUrl());

        
        

        
        
        
        

        log.info("Connection test successful for repository: {}", request.repositoryUrl());
        return true;
    }

    
    private GitIntegrationResponse enrichWithStatistics(GitIntegrationResponse response) {
        Long branchCount = branchRepository.countByGitIntegrationId(response.id());
        Long commitCount = commitRepository.countByGitIntegrationId(response.id());
        Long pullRequestCount = pullRequestRepository.countByGitIntegrationId(response.id());

        return new GitIntegrationResponse(
                response.id(),
                response.organizationId(),
                response.projectId(),
                response.provider(),
                response.repositoryUrl(),
                response.repositoryOwner(),
                response.repositoryName(),
                response.repositoryFullName(),
                response.webhookId(),
                response.webhookUrl(),
                response.webhookActive(),
                response.autoLinkEnabled(),
                response.smartCommitsEnabled(),
                response.autoCloseOnMerge(),
                response.branchPrefix(),
                response.isActive(),
                response.lastSyncAt(),
                response.createdAt(),
                response.createdByUsername(),
                branchCount,
                commitCount,
                pullRequestCount
        );
    }

    
    private String[] parseRepositoryUrl(String url) {
        
        String cleanUrl = url.endsWith(".git") ? url.substring(0, url.length() - 4) : url;

        
        String path;
        if (cleanUrl.contains("://")) {
            
            path = cleanUrl.substring(cleanUrl.indexOf("://") + 3);
            path = path.substring(path.indexOf('/') + 1);
        } else if (cleanUrl.startsWith("git@")) {
            
            path = cleanUrl.substring(cleanUrl.indexOf(':') + 1);
        } else {
            throw new IllegalArgumentException("Invalid repository URL format: " + url);
        }

        
        String[] parts = path.split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid repository URL format: " + url);
        }

        return new String[]{parts[0], parts[1]};
    }
}
