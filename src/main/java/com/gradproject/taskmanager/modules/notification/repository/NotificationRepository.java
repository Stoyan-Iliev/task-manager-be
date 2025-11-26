package com.gradproject.taskmanager.modules.notification.repository;

import com.gradproject.taskmanager.modules.notification.domain.Notification;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {


    Page<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);


    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = FALSE ORDER BY n.createdAt DESC")
    Page<Notification> findUnreadByUserId(@Param("userId") Integer userId, Pageable pageable);


    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = TRUE ORDER BY n.createdAt DESC")
    Page<Notification> findReadByUserId(@Param("userId") Integer userId, Pageable pageable);


    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(
            Integer userId,
            NotificationType type,
            Pageable pageable
    );


    Page<Notification> findByTaskIdOrderByCreatedAtDesc(Long taskId, Pageable pageable);


    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = FALSE")
    long countUnreadByUserId(@Param("userId") Integer userId);


    @Modifying
    @Query("UPDATE Notification n SET n.isRead = TRUE, n.readAt = :readAt " +
           "WHERE n.id = :notificationId AND n.user.id = :userId")
    void markAsRead(@Param("notificationId") Long notificationId,
                    @Param("userId") Integer userId,
                    @Param("readAt") LocalDateTime readAt);


    @Modifying
    @Query("UPDATE Notification n SET n.isRead = TRUE, n.readAt = :readAt " +
           "WHERE n.user.id = :userId AND n.isRead = FALSE")
    void markAllAsRead(@Param("userId") Integer userId, @Param("readAt") LocalDateTime readAt);


    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = TRUE AND n.readAt < :cutoffDate")
    int deleteReadNotificationsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);


    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.createdAt >= :since " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("userId") Integer userId,
                                                @Param("since") LocalDateTime since);

    /**
     * Find most recent notification for a specific task, user, and type.
     * Used by email notification system to link queue entries to in-app notifications.
     *
     * @param taskId Task ID
     * @param userId User ID
     * @param type Notification type
     * @return Most recent matching notification (if any)
     */
    @Query("""
        SELECT n FROM Notification n
        JOIN FETCH n.task t
        JOIN FETCH t.project
        JOIN FETCH t.organization
        LEFT JOIN FETCH n.actor
        WHERE n.task.id = :taskId
        AND n.user.id = :userId
        AND n.type = :type
        ORDER BY n.createdAt DESC
        LIMIT 1
        """)
    Notification findMostRecentByTaskAndUserAndType(
        @Param("taskId") Long taskId,
        @Param("userId") Integer userId,
        @Param("type") NotificationType type
    );
}
