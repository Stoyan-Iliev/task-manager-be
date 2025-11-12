package com.gradproject.taskmanager.modules.search.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.search.domain.SavedSearch;
import com.gradproject.taskmanager.modules.search.dto.SavedSearchRequest;
import com.gradproject.taskmanager.modules.search.dto.SavedSearchResponse;
import com.gradproject.taskmanager.modules.search.dto.SearchRequest;
import com.gradproject.taskmanager.modules.search.dto.SearchResultResponse;
import com.gradproject.taskmanager.modules.search.repository.SavedSearchRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.exception.UnauthorizedException;
import com.gradproject.taskmanager.shared.mapper.SavedSearchMapper;
import com.gradproject.taskmanager.shared.security.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final SavedSearchRepository savedSearchRepository;
    private final PermissionService permissionService;
    private final SavedSearchMapper savedSearchMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<SearchResultResponse> search(SearchRequest request, Integer userId, Long organizationId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        List<SearchResultResponse> results = new ArrayList<>();

        if (request.query() == null || request.query().isBlank()) {
            
            return new PageImpl<>(results, pageable, 0);
        }

        String entityType = request.entityType();

        
        if ("TASK".equalsIgnoreCase(entityType) || "GLOBAL".equalsIgnoreCase(entityType)) {
            List<Task> tasks = taskRepository.fullTextSearch(request.query(), organizationId);
            for (Task task : tasks) {
                
                if (permissionService.canAccessProject(user, task.getProject())) {
                    results.add(taskToSearchResult(task));
                }
            }
        }

        
        if ("PROJECT".equalsIgnoreCase(entityType) || "GLOBAL".equalsIgnoreCase(entityType)) {
            List<Project> projects = projectRepository.fullTextSearch(request.query(), organizationId);
            for (Project project : projects) {
                
                if (permissionService.canAccessProject(user, project)) {
                    results.add(projectToSearchResult(project));
                }
            }
        }

        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), results.size());
        List<SearchResultResponse> paginatedResults = start < results.size() ? results.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(paginatedResults, pageable, results.size());
    }

    @Override
    @Transactional
    public SavedSearchResponse createSavedSearch(SavedSearchRequest request, Integer userId, Long organizationId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        SavedSearch savedSearch = new SavedSearch(user, organization, request.name(), request.entityType(), request.queryParams());
        savedSearch.setDescription(request.description());
        savedSearch.setIsShared(request.isShared());

        SavedSearch saved = savedSearchRepository.save(savedSearch);
        log.info("Created saved search: {} for user: {}", saved.getId(), userId);

        return savedSearchMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SavedSearchResponse updateSavedSearch(Long id, SavedSearchRequest request, Integer userId) {
        SavedSearch savedSearch = savedSearchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Saved search not found with id: " + id));

        
        if (!savedSearch.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to update this saved search");
        }

        savedSearch.setName(request.name());
        savedSearch.setDescription(request.description());
        savedSearch.setEntityType(request.entityType());
        savedSearch.setQueryParams(request.queryParams());
        savedSearch.setIsShared(request.isShared());

        SavedSearch updated = savedSearchRepository.save(savedSearch);
        log.info("Updated saved search: {} by user: {}", id, userId);

        return savedSearchMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteSavedSearch(Long id, Integer userId) {
        SavedSearch savedSearch = savedSearchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Saved search not found with id: " + id));

        
        if (!savedSearch.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to delete this saved search");
        }

        savedSearchRepository.delete(savedSearch);
        log.info("Deleted saved search: {} by user: {}", id, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedSearchResponse> getSavedSearches(Integer userId, Long organizationId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        List<SavedSearch> savedSearches = savedSearchRepository.findAvailableSearches(organizationId, userId);

        return savedSearches.stream()
            .map(savedSearchMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SavedSearchResponse getSavedSearch(Long id, Integer userId) {
        SavedSearch savedSearch = savedSearchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Saved search not found with id: " + id));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        
        if (!savedSearch.getUser().getId().equals(userId) && !savedSearch.getIsShared()) {
            throw new UnauthorizedException("You don't have permission to view this saved search");
        }

        return savedSearchMapper.toResponse(savedSearch);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SearchResultResponse> executeSavedSearch(Long id, Integer userId, Long organizationId, Pageable pageable) {
        SavedSearch savedSearch = savedSearchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Saved search not found with id: " + id));

        
        if (!savedSearch.getUser().getId().equals(userId) && !savedSearch.getIsShared()) {
            throw new UnauthorizedException("You don't have permission to execute this saved search");
        }

        
        SearchRequest request = convertToSearchRequest(savedSearch);

        return search(request, userId, organizationId, pageable);
    }

    
    private SearchResultResponse taskToSearchResult(Task task) {
        return new SearchResultResponse(
            "TASK",
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus() != null ? task.getStatus().getName() : null,
            null, 
            List.of("title", "description"),
            task.getProject() != null ? task.getProject().getName() : null,
            task.getProject() != null ? task.getProject().getId() : null,
            task.getAssignee() != null ? task.getAssignee().getUsername() : null,
            task.getAssignee() != null ? task.getAssignee().getId() : null
        );
    }

    
    private SearchResultResponse projectToSearchResult(Project project) {
        return new SearchResultResponse(
            "PROJECT",
            project.getId(),
            project.getName(),
            project.getDescription(),
            null,
            null, 
            List.of("name", "description"),
            null,
            null,
            null,
            null
        );
    }

    
    private SearchRequest convertToSearchRequest(SavedSearch savedSearch) {
        var params = savedSearch.getQueryParams();
        return new SearchRequest(
            (String) params.get("query"),
            savedSearch.getEntityType(),
            params.containsKey("projectIds") ? (List<String>) params.get("projectIds") : null,
            params.containsKey("assigneeIds") ? (List<Integer>) params.get("assigneeIds") : null,
            params.containsKey("statuses") ? (List<String>) params.get("statuses") : null,
            params.containsKey("labelIds") ? (List<Long>) params.get("labelIds") : null,
            null, 
            null, 
            null, 
            null, 
            (Boolean) params.getOrDefault("includeArchived", false),
            (String) params.getOrDefault("sortBy", "relevance"),
            (String) params.getOrDefault("sortDirection", "DESC")
        );
    }
}
