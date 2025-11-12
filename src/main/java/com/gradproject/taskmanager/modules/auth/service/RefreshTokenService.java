package com.gradproject.taskmanager.modules.auth.service;

import com.gradproject.taskmanager.infrastructure.config.SecurityJwtProperties;
import com.gradproject.taskmanager.modules.auth.domain.RefreshToken;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.RefreshTokenRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final SecurityJwtProperties props;
    private final SecureRandom random = new SecureRandom();
    private final byte[] hmacKey;

    public RefreshTokenService(RefreshTokenRepository repo, SecurityJwtProperties props, Environment env) {
        this.repo = repo;
        this.props = props;
        String secret = props.getRefreshHmacSecret();
        boolean isProd = false;
        for (String p : env.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(p)) { isProd = true; break; }
        }
        if (secret == null || secret.isBlank()) {
            if (isProd) {
                throw new IllegalStateException("Refresh HMAC secret not configured in prod environment");
            }
            byte[] tmp = new byte[32];
            random.nextBytes(tmp);
            this.hmacKey = tmp;
        } else {
            this.hmacKey = secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String issue(User user, String userAgent, String ip) {
        String raw = generateRandomToken();
        String hash = hmacSha256(raw);
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash);
        entity.setIssuedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(props.getRefreshTokenTtl().toSeconds()));
        entity.setUserAgent(userAgent);
        entity.setIpAddress(ip);
        repo.save(entity);
        return raw;
    }

    @Transactional
    public String rotate(String rawToken, String userAgent, String ip) {
        String hash = hmacSha256(rawToken);
        RefreshToken current = repo.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("invalid_token"));
        if (current.getRevokedAt() != null) {
            throw new IllegalStateException("token_revoked");
        }
        if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("token_expired");
        }
        String newRaw = generateRandomToken();
        String newHash = hmacSha256(newRaw);
        RefreshToken replacement = new RefreshToken();
        replacement.setUser(current.getUser());
        replacement.setTokenHash(newHash);
        replacement.setIssuedAt(LocalDateTime.now());
        replacement.setExpiresAt(LocalDateTime.now().plusSeconds(props.getRefreshTokenTtl().toSeconds()));
        replacement.setUserAgent(userAgent);
        replacement.setIpAddress(ip);
        repo.save(replacement);

        current.setRevokedAt(LocalDateTime.now());
        current.setReplacedByTokenId(replacement.getId());
        repo.save(current);
        return newRaw;
    }

    @Transactional
    public void revoke(String rawToken) {
        String hash = hmacSha256(rawToken);
        repo.findByTokenHash(hash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now());
                repo.save(token);
            }
        });
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public java.util.Optional<RefreshToken> findByRawToken(String rawToken) {
        return repo.findByTokenHash(hmacSha256(rawToken));
    }

    private String hmacSha256(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            byte[] digest = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failure", e);
        }
    }
}
