package com.gradproject.taskmanager.modules.search.controller;

import com.gradproject.taskmanager.modules.search.dto.SavedSearchRequest;
import com.gradproject.taskmanager.modules.search.dto.SavedSearchResponse;
import com.gradproject.taskmanager.modules.search.dto.SearchRequest;
import com.gradproject.taskmanager.modules.search.dto.SearchResultResponse;
import com.gradproject.taskmanager.modules.search.service.SearchService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/secure")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    
    @GetMapping("/organizations/{organizationId}/search")
    public ResponseEntity<ApiResponse<Page<SearchResultResponse>>> search(
            @PathVariable Long organizationId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "GLOBAL") String entityType,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Integer userId = SecurityUtils.getCurrentUserId();

        SearchRequest request = new SearchRequest(
            query,
            entityType,
            null, null, null, null,
            null, null, null, null,
            false, "relevance", "DESC"
        );

        Pageable pageable = PageRequest.of(page, size);
        Page<SearchResultResponse> results = searchService.search(request, userId, organizationId, pageable);

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    
    @PostMapping("/organizations/{organizationId}/search")
    public ResponseEntity<ApiResponse<Page<SearchResultResponse>>> advancedSearch(
            @PathVariable Long organizationId,
            @Valid @RequestBody SearchRequest request,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<SearchResultResponse> results = searchService.search(request, userId, organizationId, pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    
    @PostMapping("/organizations/{organizationId}/saved-searches")
    public ResponseEntity<ApiResponse<SavedSearchResponse>> createSavedSearch(
            @PathVariable Long organizationId,
            @Valid @RequestBody SavedSearchRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        SavedSearchResponse response = searchService.createSavedSearch(request, userId, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping("/organizations/{organizationId}/saved-searches")
    public ResponseEntity<ApiResponse<List<SavedSearchResponse>>> getSavedSearches(
            @PathVariable Long organizationId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<SavedSearchResponse> searches = searchService.getSavedSearches(userId, organizationId);
        return ResponseEntity.ok(ApiResponse.success(searches));
    }

    
    @GetMapping("/saved-searches/{id}")
    public ResponseEntity<ApiResponse<SavedSearchResponse>> getSavedSearch(@PathVariable Long id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        SavedSearchResponse response = searchService.getSavedSearch(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PutMapping("/saved-searches/{id}")
    public ResponseEntity<ApiResponse<SavedSearchResponse>> updateSavedSearch(
            @PathVariable Long id,
            @Valid @RequestBody SavedSearchRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        SavedSearchResponse response = searchService.updateSavedSearch(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/saved-searches/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSavedSearch(@PathVariable Long id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        searchService.deleteSavedSearch(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    
    @GetMapping("/organizations/{organizationId}/saved-searches/{id}/execute")
    public ResponseEntity<ApiResponse<Page<SearchResultResponse>>> executeSavedSearch(
            @PathVariable Long organizationId,
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<SearchResultResponse> results = searchService.executeSavedSearch(id, userId, organizationId, pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
