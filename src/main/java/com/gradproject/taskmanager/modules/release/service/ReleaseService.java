package com.gradproject.taskmanager.modules.release.service;

import com.gradproject.taskmanager.modules.release.domain.enums.ReleaseStatus;
import com.gradproject.taskmanager.modules.release.dto.CreateReleaseRequest;
import com.gradproject.taskmanager.modules.release.dto.ReleaseResponse;
import com.gradproject.taskmanager.modules.release.dto.UpdateReleaseRequest;

import java.util.List;


public interface ReleaseService {

    
    ReleaseResponse createRelease(Long projectId, CreateReleaseRequest request, Integer userId);

    
    List<ReleaseResponse> getProjectReleases(Long projectId, Integer userId);

    
    List<ReleaseResponse> getProjectReleasesByStatus(Long projectId, ReleaseStatus status, Integer userId);

    
    ReleaseResponse getRelease(Long releaseId, Integer userId);

    
    ReleaseResponse updateRelease(Long releaseId, UpdateReleaseRequest request, Integer userId);

    
    void deleteRelease(Long releaseId, Integer userId);

    
    ReleaseResponse addTaskToRelease(Long releaseId, Long taskId, Integer userId);

    
    ReleaseResponse removeTaskFromRelease(Long releaseId, Long taskId, Integer userId);

    
    List<Long> getReleaseTasks(Long releaseId, Integer userId);

    
    ReleaseResponse markReleaseAsReleased(Long releaseId, Integer userId);

    
    ReleaseResponse archiveRelease(Long releaseId, Integer userId);
}
