package com.gradproject.taskmanager.modules.auth.service;

import com.gradproject.taskmanager.modules.auth.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Period;

@Component
public class TokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    private final RefreshTokenRepository repo;

    public TokenCleanupScheduler(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeOldTokens() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime revokedThreshold = now.minus(Period.ofDays(30));
        int expired = repo.deleteAllExpiredBefore(now);
        int revoked = repo.deleteAllRevokedBefore(revokedThreshold);
        if (expired > 0 || revoked > 0) {
            log.info("[TokenCleanup] Purged expired={}, revoked>30d={}", expired, revoked);
        }
    }
}
