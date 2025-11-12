package com.gradproject.taskmanager.infrastructure.security;

import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("!prod")
public class DevJwksController {

    private final KeyManager keyManager;

    public DevJwksController(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> jwks() {
        JWKSet publicSet = keyManager.jwkSet().toPublicJWKSet();
        return ResponseEntity.ok(ApiResponse.success(publicSet.toJSONObject(false)));
    }
}
