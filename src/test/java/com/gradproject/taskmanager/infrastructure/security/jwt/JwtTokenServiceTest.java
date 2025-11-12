package com.gradproject.taskmanager.infrastructure.security.jwt;

import com.gradproject.taskmanager.infrastructure.config.SecurityJwtProperties;
import com.gradproject.taskmanager.infrastructure.security.KeyManager;
import com.gradproject.taskmanager.modules.auth.domain.Role;
import com.gradproject.taskmanager.modules.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private SecurityJwtProperties props;

    @Mock
    private KeyManager keyManager;

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(jwtEncoder, props, keyManager);
    }

    @Test
    void generateAccessToken_shouldGenerateValidToken() {
        
        User user = createTestUser();
        String expectedKid = "test-kid-123";
        String expectedIssuer = "task-manager";
        List<String> expectedAudience = List.of("task-manager-api");
        Duration tokenTtl = Duration.ofMinutes(15);

        when(props.getAccessTokenTtl()).thenReturn(tokenTtl);
        when(props.getIssuer()).thenReturn(expectedIssuer);
        when(props.getAudience()).thenReturn(expectedAudience);
        when(keyManager.getCurrentKid()).thenReturn(expectedKid);

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        
        String token = jwtTokenService.generateAccessToken(user);

        
        assertThat(token).isNotNull();
        assertThat(token).isEqualTo("mock.jwt.token");

        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
        verify(keyManager).getCurrentKid();
        verify(props).getAccessTokenTtl();
        verify(props).getIssuer();
        verify(props).getAudience();
    }

    @Test
    void generateAccessToken_shouldIncludeUserRolesAndAuthorities() {
        
        User user = createTestUser();
        Duration tokenTtl = Duration.ofMinutes(15);

        when(props.getAccessTokenTtl()).thenReturn(tokenTtl);
        when(props.getIssuer()).thenReturn("task-manager");
        when(props.getAudience()).thenReturn(List.of("task-manager-api"));
        when(keyManager.getCurrentKid()).thenReturn("test-kid");

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        
        jwtTokenService.generateAccessToken(user);

        
        verify(jwtEncoder).encode(argThat(params -> {
            var claims = params.getClaims();
            assertThat((Object) claims.getClaim("roles")).isNotNull();
            assertThat((Object) claims.getClaim("authorities")).isNotNull();
            
            assertThat(claims.getSubject()).isEqualTo("1");
            
            assertThat((String) claims.getClaim("username")).isEqualTo("testuser");
            assertThat((String) claims.getClaim("email")).isEqualTo("test@example.com");
            return true;
        }));
    }

    @Test
    void generateAccessToken_shouldSetCorrectExpiry() {
        
        User user = createTestUser();
        Duration tokenTtl = Duration.ofHours(1);

        when(props.getAccessTokenTtl()).thenReturn(tokenTtl);
        when(props.getIssuer()).thenReturn("task-manager");
        when(props.getAudience()).thenReturn(List.of("task-manager-api"));
        when(keyManager.getCurrentKid()).thenReturn("test-kid");

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        Instant beforeGeneration = Instant.now();

        
        jwtTokenService.generateAccessToken(user);

        
        verify(jwtEncoder).encode(argThat(params -> {
            var claims = params.getClaims();
            Instant issuedAt = claims.getIssuedAt();
            Instant expiresAt = claims.getExpiresAt();

            assertThat(issuedAt).isNotNull();
            assertThat(expiresAt).isNotNull();
            assertThat(issuedAt).isAfterOrEqualTo(beforeGeneration);

            
            Duration actualTtl = Duration.between(issuedAt, expiresAt);
            assertThat(actualTtl).isEqualTo(tokenTtl);

            return true;
        }));
    }

    @Test
    void generateAccessToken_shouldSetCorrectAudienceAndIssuer() {
        
        User user = createTestUser();
        String expectedIssuer = "https://auth.example.com";
        List<String> expectedAudience = List.of("api-1", "api-2");

        when(props.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(props.getIssuer()).thenReturn(expectedIssuer);
        when(props.getAudience()).thenReturn(expectedAudience);
        when(keyManager.getCurrentKid()).thenReturn("test-kid");

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        
        jwtTokenService.generateAccessToken(user);

        
        verify(jwtEncoder).encode(argThat(params -> {
            var claims = params.getClaims();
            assertThat(claims.getIssuer().toString()).isEqualTo(expectedIssuer);
            assertThat(claims.getAudience()).containsExactlyElementsOf(expectedAudience);
            return true;
        }));
    }

    @Test
    void generateAccessToken_shouldUseRS256Algorithm() {
        
        User user = createTestUser();

        when(props.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(props.getIssuer()).thenReturn("task-manager");
        when(props.getAudience()).thenReturn(List.of("task-manager-api"));
        when(keyManager.getCurrentKid()).thenReturn("test-kid");

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        
        jwtTokenService.generateAccessToken(user);

        
        verify(jwtEncoder).encode(argThat(params -> {
            var header = params.getJwsHeader();
            assertThat(header.getAlgorithm()).isEqualTo(SignatureAlgorithm.RS256);
            assertThat(header.getType()).isEqualTo("JWT");
            return true;
        }));
    }

    @Test
    void generateAccessToken_shouldIncludeKeyId() {
        
        User user = createTestUser();
        String expectedKid = "unique-key-id-456";

        when(props.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(props.getIssuer()).thenReturn("task-manager");
        when(props.getAudience()).thenReturn(List.of("task-manager-api"));
        when(keyManager.getCurrentKid()).thenReturn(expectedKid);

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        
        jwtTokenService.generateAccessToken(user);

        
        verify(jwtEncoder).encode(argThat(params -> {
            var header = params.getJwsHeader();
            assertThat(header.getKeyId()).isEqualTo(expectedKid);
            return true;
        }));
    }

    @Test
    void generateAccessToken_shouldGenerateUniqueJti() {
        
        User user = createTestUser();

        when(props.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(props.getIssuer()).thenReturn("task-manager");
        when(props.getAudience()).thenReturn(List.of("task-manager-api"));
        when(keyManager.getCurrentKid()).thenReturn("test-kid");

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        
        jwtTokenService.generateAccessToken(user);

        
        verify(jwtEncoder).encode(argThat(params -> {
            var claims = params.getClaims();
            String jti = claims.getId();
            assertThat(jti).isNotNull();
            assertThat(jti).isNotEmpty();
            
            assertThat(jti).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
            return true;
        }));
    }

    @Test
    void getAccessTokenExpiresInSeconds_shouldReturnCorrectValue() {
        
        Duration tokenTtl = Duration.ofMinutes(30);
        when(props.getAccessTokenTtl()).thenReturn(tokenTtl);

        
        long expiresIn = jwtTokenService.getAccessTokenExpiresInSeconds();

        
        assertThat(expiresIn).isEqualTo(1800); 
        verify(props).getAccessTokenTtl();
    }

    @Test
    void getAccessTokenExpiresInSeconds_shouldHandleDifferentDurations() {
        
        Duration tokenTtl = Duration.ofHours(2);
        when(props.getAccessTokenTtl()).thenReturn(tokenTtl);

        
        long expiresIn = jwtTokenService.getAccessTokenExpiresInSeconds();

        
        assertThat(expiresIn).isEqualTo(7200); 
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1); 
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");

        Role role = new Role();
        role.setName("ROLE_USER");

        user.setRoles(Set.of(role));
        return user;
    }
}
