package com.gradproject.taskmanager.infrastructure.security;

import com.gradproject.taskmanager.infrastructure.config.RateLimitProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final RateLimitProperties props;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiterService(RateLimitProperties props) {
        this.props = props;
    }

    public boolean allowLogin(String ip, String username) {
        String key = "login:" + ip + ":" + username;
        return allow(key, props.getLoginMaxPerMinute());
    }

    public boolean allowRefresh(String ip, String username) {
        String key = "refresh:" + ip + ":" + username;
        return allow(key, props.getRefreshMaxPerMinute());
    }

    private boolean allow(String key, int limitPerMinute) {
        long now = Instant.now().getEpochSecond();
        long currentWindow = now / 60; 
                
        Window w = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.window != currentWindow) {
                return new Window(currentWindow, 0);
            }
            return existing;
        });
        
        int next = w.count + 1;
        boolean allowed = next <= limitPerMinute;
        
        w.count = Math.min(next, limitPerMinute + 1);
        return allowed;
    }

    private static class Window {
        final long window;
        int count;
        Window(long window, int count) {
            this.window = window;
            this.count = count;
        }
    }
}
