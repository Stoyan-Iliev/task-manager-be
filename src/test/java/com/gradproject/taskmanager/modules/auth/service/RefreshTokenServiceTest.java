package com.gradproject.taskmanager.modules.auth.service;

import com.gradproject.taskmanager.AbstractIntegrationTest;
import com.gradproject.taskmanager.modules.auth.domain.RefreshToken;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.RefreshTokenRepository;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


class RefreshTokenServiceTests extends AbstractIntegrationTest {

    @Autowired
    RefreshTokenService refreshTokenService;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private User ensureUser() {
        User u = new User("refresh_tester", "refresh_tester@example.com", passwordEncoder.encode("Password1"));
        return userRepository.save(u);
    }

    @Test
    @Transactional
    void rotate_isAtomic_andReuseRejected() {
        User user = ensureUser();
        String ua = "JUnit";
        String ip = "127.0.0.1";

        String raw1 = refreshTokenService.issue(user, ua, ip);
        Optional<RefreshToken> firstOpt = refreshTokenService.findByRawToken(raw1);
        Assertions.assertTrue(firstOpt.isPresent(), "Issued token should be persisted and discoverable by raw value");
        RefreshToken first = firstOpt.get();
        String storedHash1 = first.getTokenHash();
        Assertions.assertNotEquals(storedHash1, raw1, "Token hash must not equal raw token");

        String raw2 = refreshTokenService.rotate(raw1, ua, ip);
        RefreshToken updatedFirst = refreshTokenRepository.findById(first.getId()).orElseThrow();
        Assertions.assertNotNull(updatedFirst.getRevokedAt(), "Old token must be revoked");
        Assertions.assertNotNull(updatedFirst.getReplacedByTokenId(), "Old token must link to replacement");

        Optional<RefreshToken> secondOpt = refreshTokenService.findByRawToken(raw2);
        Assertions.assertTrue(secondOpt.isPresent(), "Rotated token must exist");

        
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                () -> refreshTokenService.rotate(raw1, ua, ip));
        Assertions.assertEquals("token_revoked", ex.getMessage());
    }
}
