package com.gradproject.taskmanager.infrastructure.health;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class HealthService {

    public Map<String, Object> buildHealth() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "UP");
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("app", "taskmanager-backend");
        return body;
    }
}
