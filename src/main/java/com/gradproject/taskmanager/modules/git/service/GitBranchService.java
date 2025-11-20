package com.gradproject.taskmanager.modules.git.service;

import com.gradproject.taskmanager.modules.git.dto.request.CreateBranchRequest;
import com.gradproject.taskmanager.modules.git.dto.response.BranchResponse;

import java.util.List;

public interface GitBranchService {

    /**
     * Create a new branch for a task
     */
    BranchResponse createBranch(Long taskId, CreateBranchRequest request, Integer userId);

    /**
     * Get all branches for a task
     */
    List<BranchResponse> getBranchesByTask(Long taskId, Integer userId);

    /**
     * Get all branches for a project
     */
    List<BranchResponse> getBranchesByProject(Long projectId, Integer userId);

    /**
     * Get all branches for a Git integration
     */
    List<BranchResponse> getBranchesByIntegration(Long integrationId, Integer userId);

    /**
     * Get a specific branch by ID
     */
    BranchResponse getBranch(Long branchId, Integer userId);

    /**
     * Delete a branch
     */
    void deleteBranch(Long branchId, Integer userId);

    /**
     * Get active branches for a task
     */
    List<BranchResponse> getActiveBranchesByTask(Long taskId, Integer userId);
}
