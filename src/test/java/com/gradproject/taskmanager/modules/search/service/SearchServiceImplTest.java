package com.gradproject.taskmanager.modules.search.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SavedSearchRepository savedSearchRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private SavedSearchMapper savedSearchMapper;

    @InjectMocks
    private SearchServiceImpl searchService;

    private User testUser;
    private Organization testOrganization;
    private Project testProject;
    private Task testTask;
    private TaskStatus testStatus;
    private SavedSearch testSavedSearch;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Test Project");
        testProject.setDescription("Test Description");
        testProject.setOrganization(testOrganization);

        testStatus = new TaskStatus();
        testStatus.setId(1L);
        testStatus.setName("To Do");

        testTask = new Task();
        testTask.setId(1L);
        testTask.setTitle("Test Task");
        testTask.setDescription("Test Task Description");
        testTask.setProject(testProject);
        testTask.setStatus(testStatus);
        testTask.setAssignee(testUser);
        testTask.setOrganization(testOrganization);

        testSavedSearch = new SavedSearch(testUser, testOrganization, "My Search", "TASK", new HashMap<>());
        testSavedSearch.setId(1L);
        testSavedSearch.setIsShared(false);
    }

    @Test
    void search_withTaskQuery_returnsTaskResults() {
        
        SearchRequest request = new SearchRequest(
            "test query", "TASK", null, null, null, null,
            null, null, null, null, false, "relevance", "DESC"
        );
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(taskRepository.fullTextSearch("test query", 1L)).thenReturn(List.of(testTask));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);

        
        Page<SearchResultResponse> results = searchService.search(request, 1, 1L, pageable);

        
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).entityType()).isEqualTo("TASK");
        assertThat(results.getContent().get(0).title()).isEqualTo("Test Task");

        verify(taskRepository).fullTextSearch("test query", 1L);
        verify(permissionService).canAccessProject(testUser, testProject);
        verify(projectRepository, never()).fullTextSearch(anyString(), anyLong());
    }

    @Test
    void search_withProjectQuery_returnsProjectResults() {
        
        SearchRequest request = new SearchRequest(
            "test query", "PROJECT", null, null, null, null,
            null, null, null, null, false, "relevance", "DESC"
        );
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(projectRepository.fullTextSearch("test query", 1L)).thenReturn(List.of(testProject));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);

        
        Page<SearchResultResponse> results = searchService.search(request, 1, 1L, pageable);

        
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).entityType()).isEqualTo("PROJECT");
        assertThat(results.getContent().get(0).title()).isEqualTo("Test Project");

        verify(projectRepository).fullTextSearch("test query", 1L);
        verify(permissionService).canAccessProject(testUser, testProject);
        verify(taskRepository, never()).fullTextSearch(anyString(), anyLong());
    }

    @Test
    void search_withGlobalQuery_returnsTasksAndProjects() {
        
        SearchRequest request = new SearchRequest(
            "test query", "GLOBAL", null, null, null, null,
            null, null, null, null, false, "relevance", "DESC"
        );
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(taskRepository.fullTextSearch("test query", 1L)).thenReturn(List.of(testTask));
        when(projectRepository.fullTextSearch("test query", 1L)).thenReturn(List.of(testProject));
        when(permissionService.canAccessProject(eq(testUser), any(Project.class))).thenReturn(true);

        
        Page<SearchResultResponse> results = searchService.search(request, 1, 1L, pageable);

        
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(2);

        verify(taskRepository).fullTextSearch("test query", 1L);
        verify(projectRepository).fullTextSearch("test query", 1L);
        verify(permissionService, times(2)).canAccessProject(eq(testUser), any(Project.class));
    }

    @Test
    void search_withEmptyQuery_returnsEmptyResults() {
        
        SearchRequest request = new SearchRequest(
            "", "GLOBAL", null, null, null, null,
            null, null, null, null, false, "relevance", "DESC"
        );
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

        
        Page<SearchResultResponse> results = searchService.search(request, 1, 1L, pageable);

        
        assertThat(results).isNotNull();
        assertThat(results.getContent()).isEmpty();

        verify(taskRepository, never()).fullTextSearch(anyString(), anyLong());
        verify(projectRepository, never()).fullTextSearch(anyString(), anyLong());
    }

    @Test
    void search_filtersOutUnauthorizedResults() {
        
        SearchRequest request = new SearchRequest(
            "test query", "TASK", null, null, null, null,
            null, null, null, null, false, "relevance", "DESC"
        );
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(taskRepository.fullTextSearch("test query", 1L)).thenReturn(List.of(testTask));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(false);

        
        Page<SearchResultResponse> results = searchService.search(request, 1, 1L, pageable);

        
        assertThat(results).isNotNull();
        assertThat(results.getContent()).isEmpty();

        verify(permissionService).canAccessProject(testUser, testProject);
    }

    @Test
    void search_withInvalidUser_throwsResourceNotFoundException() {
        
        SearchRequest request = new SearchRequest(
            "test query", "TASK", null, null, null, null,
            null, null, null, null, false, "relevance", "DESC"
        );
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findById(999)).thenReturn(Optional.empty());

        
        assertThatThrownBy(() -> searchService.search(request, 999, 1L, pageable))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void createSavedSearch_success() {
        
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("query", "test");
        SavedSearchRequest request = new SavedSearchRequest(
            "My Saved Search", "Quick search for bugs", "TASK", queryParams, false
        );

        SavedSearchResponse expectedResponse = new SavedSearchResponse(
            1L, "My Saved Search", "Quick search for bugs", "TASK",
            queryParams, false, 1, "testuser", 1L,
            LocalDateTime.now(), null
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(savedSearchRepository.save(any(SavedSearch.class))).thenReturn(testSavedSearch);
        when(savedSearchMapper.toResponse(testSavedSearch)).thenReturn(expectedResponse);

        
        SavedSearchResponse response = searchService.createSavedSearch(request, 1, 1L);

        
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("My Saved Search");

        verify(savedSearchRepository).save(any(SavedSearch.class));
        verify(savedSearchMapper).toResponse(testSavedSearch);
    }

    @Test
    void updateSavedSearch_success() {
        
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("query", "updated");
        SavedSearchRequest request = new SavedSearchRequest(
            "Updated Name", "Updated description", "TASK", queryParams, true
        );

        SavedSearchResponse expectedResponse = new SavedSearchResponse(
            1L, "Updated Name", "Updated description", "TASK",
            queryParams, true, 1, "testuser", 1L,
            LocalDateTime.now(), LocalDateTime.now()
        );

        when(savedSearchRepository.findById(1L)).thenReturn(Optional.of(testSavedSearch));
        when(savedSearchRepository.save(any(SavedSearch.class))).thenReturn(testSavedSearch);
        when(savedSearchMapper.toResponse(testSavedSearch)).thenReturn(expectedResponse);

        
        SavedSearchResponse response = searchService.updateSavedSearch(1L, request, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Updated Name");

        verify(savedSearchRepository).save(testSavedSearch);
    }

    @Test
    void updateSavedSearch_withUnauthorizedUser_throwsUnauthorizedException() {
        
        User otherUser = new User();
        otherUser.setId(2);
        testSavedSearch.setUser(otherUser);

        SavedSearchRequest request = new SavedSearchRequest(
            "Updated Name", null, "TASK", new HashMap<>(), false
        );

        when(savedSearchRepository.findById(1L)).thenReturn(Optional.of(testSavedSearch));

        
        assertThatThrownBy(() -> searchService.updateSavedSearch(1L, request, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("You don't have permission to update this saved search");
    }

    @Test
    void deleteSavedSearch_success() {
        
        when(savedSearchRepository.findById(1L)).thenReturn(Optional.of(testSavedSearch));

        
        searchService.deleteSavedSearch(1L, 1);

        
        verify(savedSearchRepository).delete(testSavedSearch);
    }

    @Test
    void deleteSavedSearch_withUnauthorizedUser_throwsUnauthorizedException() {
        
        User otherUser = new User();
        otherUser.setId(2);
        testSavedSearch.setUser(otherUser);

        when(savedSearchRepository.findById(1L)).thenReturn(Optional.of(testSavedSearch));

        
        assertThatThrownBy(() -> searchService.deleteSavedSearch(1L, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("You don't have permission to delete this saved search");
    }

    @Test
    void getSavedSearches_returnsUserAndSharedSearches() {
        
        SavedSearch sharedSearch = new SavedSearch(testUser, testOrganization, "Shared Search", "TASK", new HashMap<>());
        sharedSearch.setId(2L);
        sharedSearch.setIsShared(true);

        List<SavedSearch> searches = List.of(testSavedSearch, sharedSearch);

        SavedSearchResponse response1 = new SavedSearchResponse(
            1L, "My Search", null, "TASK", new HashMap<>(), false,
            1, "testuser", 1L, LocalDateTime.now(), null
        );
        SavedSearchResponse response2 = new SavedSearchResponse(
            2L, "Shared Search", null, "TASK", new HashMap<>(), true,
            1, "testuser", 1L, LocalDateTime.now(), null
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(savedSearchRepository.findAvailableSearches(1L, 1)).thenReturn(searches);
        when(savedSearchMapper.toResponse(testSavedSearch)).thenReturn(response1);
        when(savedSearchMapper.toResponse(sharedSearch)).thenReturn(response2);

        
        List<SavedSearchResponse> results = searchService.getSavedSearches(1, 1L);

        
        assertThat(results).hasSize(2);
        verify(savedSearchRepository).findAvailableSearches(1L, 1);
    }

    @Test
    void getSavedSearch_success() {
        
        SavedSearchResponse expectedResponse = new SavedSearchResponse(
            1L, "My Search", null, "TASK", new HashMap<>(), false,
            1, "testuser", 1L, LocalDateTime.now(), null
        );

        when(savedSearchRepository.findById(1L)).thenReturn(Optional.of(testSavedSearch));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(savedSearchMapper.toResponse(testSavedSearch)).thenReturn(expectedResponse);

        
        SavedSearchResponse response = searchService.getSavedSearch(1L, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void getSavedSearch_withUnauthorizedUser_throwsUnauthorizedException() {
        
        User otherUser = new User();
        otherUser.setId(2);
        testSavedSearch.setUser(otherUser);
        testSavedSearch.setIsShared(false);

        when(savedSearchRepository.findById(1L)).thenReturn(Optional.of(testSavedSearch));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        
        assertThatThrownBy(() -> searchService.getSavedSearch(1L, 1))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("You don't have permission to view this saved search");
    }

    @Test
    void getSavedSearch_sharedSearch_success() {
        
        User otherUser = new User();
        otherUser.setId(2);
        testSavedSearch.setUser(otherUser);
        testSavedSearch.setIsShared(true);

        SavedSearchResponse expectedResponse = new SavedSearchResponse(
            1L, "My Search", null, "TASK", new HashMap<>(), true,
            2, "otheruser", 1L, LocalDateTime.now(), null
        );

        when(savedSearchRepository.findById(1L)).thenReturn(Optional.of(testSavedSearch));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(savedSearchMapper.toResponse(testSavedSearch)).thenReturn(expectedResponse);

        
        SavedSearchResponse response = searchService.getSavedSearch(1L, 1);

        
        assertThat(response).isNotNull();
        assertThat(response.isShared()).isTrue();
    }

    @Test
    void executeSavedSearch_success() {
        
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("query", "test");
        queryParams.put("sortBy", "relevance");
        queryParams.put("sortDirection", "DESC");
        queryParams.put("includeArchived", false);

        testSavedSearch.setQueryParams(queryParams);

        Pageable pageable = PageRequest.of(0, 20);

        when(savedSearchRepository.findById(1L)).thenReturn(Optional.of(testSavedSearch));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(taskRepository.fullTextSearch("test", 1L)).thenReturn(List.of(testTask));
        when(permissionService.canAccessProject(testUser, testProject)).thenReturn(true);

        
        Page<SearchResultResponse> results = searchService.executeSavedSearch(1L, 1, 1L, pageable);

        
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);

        verify(taskRepository).fullTextSearch("test", 1L);
    }

    @Test
    void executeSavedSearch_withUnauthorizedUser_throwsUnauthorizedException() {
        
        User otherUser = new User();
        otherUser.setId(2);
        testSavedSearch.setUser(otherUser);
        testSavedSearch.setIsShared(false);

        when(savedSearchRepository.findById(1L)).thenReturn(Optional.of(testSavedSearch));

        
        assertThatThrownBy(() -> searchService.executeSavedSearch(1L, 1, 1L, PageRequest.of(0, 20)))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("You don't have permission to execute this saved search");
    }
}
