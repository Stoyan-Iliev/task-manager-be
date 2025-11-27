package com.gradproject.taskmanager.modules.task.dto;


public record WorkLogSummary(
        Long taskId,
        Integer totalTimeSpentMinutes,
        String totalTimeSpentFormatted,
        Integer logCount
) {
    /**
     * Get total time spent in hours (decimal).
     */
    public double getTotalTimeSpentHours() {
        return totalTimeSpentMinutes != null ? totalTimeSpentMinutes / 60.0 : 0;
    }

    /**
     * Check if any time has been logged.
     */
    public boolean hasLoggedTime() {
        return totalTimeSpentMinutes != null && totalTimeSpentMinutes > 0;
    }

    /**
     * Create an empty summary for a task with no work logs.
     */
    public static WorkLogSummary empty(Long taskId) {
        return new WorkLogSummary(taskId, 0, "0m", 0);
    }
}
