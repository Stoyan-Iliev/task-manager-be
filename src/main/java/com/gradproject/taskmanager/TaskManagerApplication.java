package com.gradproject.taskmanager;

import com.gradproject.taskmanager.infrastructure.config.CorsProperties;
import com.gradproject.taskmanager.infrastructure.config.RateLimitProperties;
import com.gradproject.taskmanager.infrastructure.config.SecurityJwtProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties({SecurityJwtProperties.class, CorsProperties.class, RateLimitProperties.class})
@EnableScheduling
public class TaskManagerApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Sofia"));
    }

    public static void main(String[] args) {
        SpringApplication.run(TaskManagerApplication.class, args);
    }
}
