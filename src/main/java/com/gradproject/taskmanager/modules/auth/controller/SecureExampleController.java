package com.gradproject.taskmanager.modules.auth.controller;

import com.gradproject.taskmanager.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/secure")
public class SecureExampleController {

    @GetMapping("/example")
    public ResponseEntity<ApiResponse<Map<String, Object>>> example() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "secure-ok")));
    }
}
