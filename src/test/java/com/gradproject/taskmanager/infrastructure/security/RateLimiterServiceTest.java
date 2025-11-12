package com.gradproject.taskmanager.infrastructure.security;

import com.gradproject.taskmanager.AbstractIntegrationTest;
import com.gradproject.taskmanager.infrastructure.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class RateLimiterServiceTests extends AbstractIntegrationTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setup() {
        RateLimitProperties props = new RateLimitProperties();
        props.setLoginMaxPerMinute(2);
        props.setRefreshMaxPerMinute(2);
        rateLimiterService = new RateLimiterService(props);
    }

    @Test
    void login_rate_limit_per_ip_and_user() {
        String ip1 = "127.0.0.1";
        String userA = "alice";
        assertThat(rateLimiterService.allowLogin(ip1, userA)).isTrue();
        assertThat(rateLimiterService.allowLogin(ip1, userA)).isTrue();
        
        assertThat(rateLimiterService.allowLogin(ip1, userA)).isFalse();

        
        assertThat(rateLimiterService.allowLogin(ip1, "bob")).isTrue();
    }

    @Test
    void refresh_rate_limit_per_ip_and_user() {
        String ip1 = "127.0.0.1";
        String userA = "alice";
        assertThat(rateLimiterService.allowRefresh(ip1, userA)).isTrue();
        assertThat(rateLimiterService.allowRefresh(ip1, userA)).isTrue();
        assertThat(rateLimiterService.allowRefresh(ip1, userA)).isFalse();
    }
}
