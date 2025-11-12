package com.gradproject.taskmanager.modules.project.dto;

import java.math.BigDecimal;


public record SprintMetrics(
    int totalTasks,
    int completedTasks,
    int remainingTasks,
    BigDecimal totalPoints,
    BigDecimal completedPoints,
    double progressPercentage
) {
    
    public static SprintMetrics calculate(
            int totalTasks,
            int completedTasks,
            BigDecimal totalPoints,
            BigDecimal completedPoints
    ) {
        int remainingTasks = totalTasks - completedTasks;
        double progressPercentage = totalTasks > 0
                ? (completedTasks * 100.0) / totalTasks
                : 0.0;

        return new SprintMetrics(
                totalTasks,
                completedTasks,
                remainingTasks,
                totalPoints != null ? totalPoints : BigDecimal.ZERO,
                completedPoints != null ? completedPoints : BigDecimal.ZERO,
                Math.round(progressPercentage * 10.0) / 10.0  
        );
    }
}
