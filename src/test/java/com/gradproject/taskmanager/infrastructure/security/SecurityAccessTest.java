package com.gradproject.taskmanager.infrastructure.security;

import com.gradproject.taskmanager.AbstractIntegrationTest;
import com.gradproject.taskmanager.infrastructure.config.SecurityJwtProperties;
import com.gradproject.taskmanager.infrastructure.security.jwt.JwtTokenService;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
class SecurityAccessTests extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired
    JwtTokenService jwtTokenService;
    @Autowired UserRepository userRepository;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired SecurityJwtProperties props;

    @Test
    void public_health_is_accessible() throws Exception {
        mockMvc.perform(get("/api/public/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void protected_endpoint_requires_bearer_token() throws Exception {
        mockMvc.perform(get("/api/secure/example").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.error").value("invalid_token"));
    }

    @Test
    void valid_jwt_grants_access() throws Exception {
        User u = userRepository.save(new User("secuser", "secuser@example.com", "x"));
        String token = jwtTokenService.generateAccessToken(u);
        mockMvc.perform(get("/api/secure/example")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("secure-ok"));
    }

    @Test
    void expired_jwt_rejected() throws Exception {
        
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.getIssuer())
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60))
                .subject("999") 
                .claim("username", "expired-user")
                .audience(props.getAudience())
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        mockMvc.perform(get("/api/secure/example")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
                .andExpect(jsonPath("$.error").value("invalid_token"));
    }
}
