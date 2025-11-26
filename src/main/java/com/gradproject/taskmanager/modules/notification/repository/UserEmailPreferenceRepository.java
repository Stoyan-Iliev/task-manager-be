package com.gradproject.taskmanager.modules.notification.repository;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.UserEmailPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for user email preferences.
 */
@Repository
public interface UserEmailPreferenceRepository extends JpaRepository<UserEmailPreference, Long> {

    /**
     * Find email preferences for a specific user.
     *
     * @param user the user to find preferences for
     * @return optional containing preferences if found
     */
    Optional<UserEmailPreference> findByUser(User user);

    /**
     * Find email preferences by user ID.
     *
     * @param userId the user ID
     * @return optional containing preferences if found
     */
    @Query("SELECT p FROM UserEmailPreference p WHERE p.user.id = :userId")
    Optional<UserEmailPreference> findByUserId(@Param("userId") Integer userId);

    /**
     * Check if preferences exist for a user.
     *
     * @param user the user to check
     * @return true if preferences exist
     */
    boolean existsByUser(User user);

    /**
     * Delete preferences for a specific user.
     *
     * @param user the user whose preferences to delete
     */
    void deleteByUser(User user);
}
