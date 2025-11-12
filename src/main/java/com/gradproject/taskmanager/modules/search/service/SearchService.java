package com.gradproject.taskmanager.modules.search.service;

import com.gradproject.taskmanager.modules.search.dto.SavedSearchRequest;
import com.gradproject.taskmanager.modules.search.dto.SavedSearchResponse;
import com.gradproject.taskmanager.modules.search.dto.SearchRequest;
import com.gradproject.taskmanager.modules.search.dto.SearchResultResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface SearchService {

    
    Page<SearchResultResponse> search(SearchRequest request, Integer userId, Long organizationId, Pageable pageable);

    
    SavedSearchResponse createSavedSearch(SavedSearchRequest request, Integer userId, Long organizationId);

    
    SavedSearchResponse updateSavedSearch(Long id, SavedSearchRequest request, Integer userId);

    
    void deleteSavedSearch(Long id, Integer userId);

    
    List<SavedSearchResponse> getSavedSearches(Integer userId, Long organizationId);

    
    SavedSearchResponse getSavedSearch(Long id, Integer userId);

    
    Page<SearchResultResponse> executeSavedSearch(Long id, Integer userId, Long organizationId, Pageable pageable);
}
