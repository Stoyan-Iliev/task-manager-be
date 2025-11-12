package com.gradproject.taskmanager.infrastructure.config.properties;

import com.gradproject.taskmanager.AbstractIntegrationTest;
import com.gradproject.taskmanager.infrastructure.config.CorsProperties;
import com.gradproject.taskmanager.infrastructure.config.RateLimitProperties;
import com.gradproject.taskmanager.infrastructure.config.SecurityJwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;


class PropertiesBindingTests extends AbstractIntegrationTest {

    @Autowired
    SecurityJwtProperties jwtProps;
    @Autowired
    CorsProperties corsProps;
    @Autowired
    RateLimitProperties rateProps;

    @Test
    void securityJwtProperties_bind_defaults() {
        assertThat(jwtProps.getIssuer()).isEqualTo("http://localhost:8080");
        assertThat(jwtProps.getAudience()).contains("web");
        assertThat(jwtProps.getAccessTokenTtl()).isNotNull();
        assertThat(jwtProps.getRefreshTokenTtl()).isNotNull();
        assertThat(jwtProps.getClockSkew()).isNotNull();
    }

    @Test
    void corsProperties_bind_defaults() {
        assertThat(corsProps.getAllowedOrigins()).contains("http://localhost:3000");
        assertThat(corsProps.getAllowedHeaders()).contains("Authorization", "Content-Type");
    }

    @Test
    void rateLimitProperties_bind_defaults() {
        assertThat(rateProps.getLoginMaxPerMinute()).isGreaterThan(0);
        assertThat(rateProps.getRefreshMaxPerMinute()).isGreaterThan(0);
    }
}
