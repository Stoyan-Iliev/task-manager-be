package com.gradproject.taskmanager.infrastructure.scheduler;

import com.gradproject.taskmanager.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    private final NotificationService notificationService;

    
    @Value("${app.notifications.retention-days:30}")
    private int retentionDays;

    
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldNotifications() {
        log.info("Starting scheduled cleanup of old read notifications (older than {} days)", retentionDays);

        try {
            int deletedCount = notificationService.cleanupOldNotifications(retentionDays);
            log.info("Scheduled cleanup completed. Deleted {} old read notifications", deletedCount);
        } catch (Exception e) {
            log.error("Error during scheduled notification cleanup", e);
        }
    }

    
    
    
    
    
}
