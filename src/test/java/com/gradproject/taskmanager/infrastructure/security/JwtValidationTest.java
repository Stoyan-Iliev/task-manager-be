package com.gradproject.taskmanager.infrastructure.security;

import com.gradproject.taskmanager.AbstractIntegrationTest;
import com.gradproject.taskmanager.infrastructure.config.SecurityJwtProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


class JwtValidationTests extends AbstractIntegrationTest {

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    JwtDecoder jwtDecoder;

    @Autowired
    SecurityJwtProperties props;

    private String encodeToken(String issuer, List<String> audience) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .subject("123") 
                .claim("username", "alice")
                .audience(audience)
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    void validToken_decodes() {
        String token = encodeToken(props.getIssuer(), props.getAudience());
        Jwt jwt = jwtDecoder.decode(token);
        Assertions.assertEquals("123", jwt.getSubject());
        Assertions.assertEquals("alice", jwt.getClaim("username"));
    }

    @Test
    void invalidIssuer_rejected() {
        String token = encodeToken("https://invalid-issuer", props.getAudience());
        Assertions.assertThrows(JwtException.class, () -> jwtDecoder.decode(token));
    }

    @Test
    void invalidAudience_rejected() {
        String token = encodeToken(props.getIssuer(), List.of("mobile"));
        Assertions.assertThrows(JwtException.class, () -> jwtDecoder.decode(token));
    }
}
