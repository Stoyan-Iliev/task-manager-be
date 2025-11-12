package com.gradproject.taskmanager.infrastructure.security;

import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevJwksControllerTest {

    @Mock
    private KeyManager keyManager;

    @Mock
    private JWKSet jwkSet;

    @Mock
    private JWKSet publicJwkSet;

    private DevJwksController controller;

    @BeforeEach
    void setUp() {
        controller = new DevJwksController(keyManager);
    }

    @Test
    void jwks_shouldReturnPublicJwks() {
        
        Map<String, Object> expectedKeys = Map.of(
                "keys", java.util.List.of(
                        Map.of("kty", "RSA", "kid", "test-key-id")
                )
        );

        when(keyManager.jwkSet()).thenReturn(jwkSet);
        when(jwkSet.toPublicJWKSet()).thenReturn(publicJwkSet);
        when(publicJwkSet.toJSONObject(false)).thenReturn(expectedKeys);

        
        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.jwks();

        
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo(expectedKeys);
    }

    @Test
    void jwks_shouldReturnOkStatus() {
        
        Map<String, Object> keys = Map.of("keys", java.util.List.of());

        when(keyManager.jwkSet()).thenReturn(jwkSet);
        when(jwkSet.toPublicJWKSet()).thenReturn(publicJwkSet);
        when(publicJwkSet.toJSONObject(false)).thenReturn(keys);

        
        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.jwks();

        
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void jwks_shouldIncludeOnlyPublicKeys() {
        
        when(keyManager.jwkSet()).thenReturn(jwkSet);
        when(jwkSet.toPublicJWKSet()).thenReturn(publicJwkSet);
        when(publicJwkSet.toJSONObject(false)).thenReturn(Map.of());

        
        controller.jwks();

        
        org.mockito.Mockito.verify(jwkSet).toPublicJWKSet();
    }
}
