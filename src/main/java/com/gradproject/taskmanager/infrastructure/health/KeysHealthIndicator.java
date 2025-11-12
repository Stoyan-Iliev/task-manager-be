package com.gradproject.taskmanager.infrastructure.health;

import com.gradproject.taskmanager.infrastructure.security.KeyManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("securityKeys")
public class KeysHealthIndicator implements HealthIndicator {

    private final KeyManager keyManager;

    public KeysHealthIndicator(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    @Override
    public Health health() {
        boolean loaded = keyManager.keysLoaded();
        if (loaded) {
            return Health.up().withDetail("keysLoaded", true).build();
        }
        return Health.down().withDetail("keysLoaded", false).build();
    }
}
