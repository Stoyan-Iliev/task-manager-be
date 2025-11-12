package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.dto.response.PullRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface GitPullRequestService {

    
    List<PullRequestResponse> getPullRequestsByTask(Long taskId, Integer userId);

    
    Page<PullRequestResponse> getPullRequestsByProject(Long projectId, Integer userId, Pageable pageable);

    
    PullRequestResponse getPullRequest(Long prId, Integer userId);

    
    PullRequestResponse getPullRequestByNumber(Long integrationId, Integer prNumber, Integer userId);

    
    PullRequestResponse linkPullRequestToTask(Long prId, Long taskId, Boolean closesTask, Integer userId);

    
    PullRequestResponse unlinkPullRequestFromTask(Long prId, Long taskId, Integer userId);
}
