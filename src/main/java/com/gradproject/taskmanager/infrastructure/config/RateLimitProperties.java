package com.gradproject.taskmanager.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "security.limits")
public class RateLimitProperties {
    private int loginMaxPerMinute = 5;
    private int refreshMaxPerMinute = 20;

}
