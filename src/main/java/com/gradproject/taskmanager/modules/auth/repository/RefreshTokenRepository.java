package com.gradproject.taskmanager.modules.auth.repository;

import com.gradproject.taskmanager.modules.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    @Query("select rt from RefreshToken rt join fetch rt.user" +
            " where rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from RefreshToken rt where rt.expiresAt < :threshold")
    int deleteAllExpiredBefore(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("delete from RefreshToken rt where rt.revokedAt is not null and rt.revokedAt < :threshold")
    int deleteAllRevokedBefore(@Param("threshold") LocalDateTime threshold);
}
