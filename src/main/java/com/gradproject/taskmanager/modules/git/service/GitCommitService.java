package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.dto.response.CommitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface GitCommitService {

    
    List<CommitResponse> getCommitsByTask(Long taskId, Integer userId);

    
    Page<CommitResponse> getCommitsByTask(Long taskId, Integer userId, Pageable pageable);

    
    Page<CommitResponse> getCommitsByProject(Long projectId, Integer userId, Pageable pageable);

    
    CommitResponse getCommit(Long commitId, Integer userId);

    
    CommitResponse getCommitBySha(Long integrationId, String commitSha, Integer userId);

    
    CommitResponse linkCommitToTask(Long commitId, Long taskId, Integer userId);

    
    CommitResponse unlinkCommitFromTask(Long commitId, Long taskId, Integer userId);
}
