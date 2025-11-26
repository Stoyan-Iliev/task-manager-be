package com.gradproject.taskmanager.modules.task.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;


public record WorkLogRequest(
        @NotNull(message = "Time spent is required")
        @Min(value = 1, message = "Time spent must be at least 1 minute")
        Integer timeSpentMinutes,

        LocalDate workDate,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description
) {
    /**
     * Creates a request with just time spent (for smart commits).
     */
    public static WorkLogRequest ofMinutes(int minutes) {
        return new WorkLogRequest(minutes, null, null);
    }

    /**
     * Creates a request with time and description (for smart commits with work description).
     */
    public static WorkLogRequest ofMinutes(int minutes, String description) {
        return new WorkLogRequest(minutes, null, description);
    }

    /**
     * Get the work date, defaulting to today if not specified.
     */
    public LocalDate getWorkDateOrToday() {
        return workDate != null ? workDate : LocalDate.now();
    }

    /**
     * Get trimmed description or null.
     */
    public String getTrimmedDescription() {
        return description != null ? description.trim() : null;
    }
}
