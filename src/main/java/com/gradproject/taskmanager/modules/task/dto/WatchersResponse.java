package com.gradproject.taskmanager.modules.task.dto;

import com.gradproject.taskmanager.shared.dto.UserSummary;

import java.util.List;


public record WatchersResponse(
    List<UserSummary> watchers,
    int totalCount,
    boolean isCurrentUserWatching
) {}
