package com.gradproject.taskmanager.modules.task.dto;

import com.gradproject.taskmanager.modules.task.domain.WorkLogSource;
import com.gradproject.taskmanager.shared.dto.UserSummary;

import java.time.LocalDate;
import java.time.LocalDateTime;


public record WorkLogResponse(
        Long id,
        Long taskId,
        String taskKey,
        UserSummary author,
        Integer timeSpentMinutes,
        String timeSpentFormatted,
        LocalDate workDate,
        String description,
        WorkLogSource source,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Check if this work log has a description.
     */
    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    /**
     * Get time spent in hours (decimal).
     */
    public double getTimeSpentHours() {
        return timeSpentMinutes != null ? timeSpentMinutes / 60.0 : 0;
    }
}
