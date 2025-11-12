package com.gradproject.taskmanager.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthConverterTest {

    private JwtAuthConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JwtAuthConverter();
    }

    @Test
    void convert_shouldExtractAuthoritiesFromJwt() {
        
        List<String> authorities = List.of("ROLE_USER", "ROLE_ADMIN");
        Jwt jwt = createJwtWithAuthorities(authorities);

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).hasSize(2);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void convert_shouldSetPrincipalFromSubject() {
        
        String expectedSubject = "testuser";
        Jwt jwt = createJwtWithSubject(expectedSubject, List.of("ROLE_USER"));

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(expectedSubject);
    }

    @Test
    void convert_shouldHandleEmptyAuthoritiesList() {
        
        Jwt jwt = createJwtWithAuthorities(List.of());

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void convert_shouldHandleMissingAuthoritiesClaim() {
        
        Jwt jwt = createJwtWithoutAuthorities();

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void convert_shouldHandleSingleAuthority() {
        
        List<String> authorities = List.of("ROLE_USER");
        Jwt jwt = createJwtWithAuthorities(authorities);

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void convert_shouldHandleMultipleAuthorities() {
        
        List<String> authorities = List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_MODERATOR");
        Jwt jwt = createJwtWithAuthorities(authorities);

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).hasSize(3);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_MODERATOR");
    }

    @Test
    void convert_shouldFilterNonStringAuthorities() {
        
        Jwt jwt = createJwtWithMixedAuthorities();

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void convert_shouldHandleNullAuthoritiesClaim() {
        
        Jwt jwt = createJwtWithNullAuthorities();

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void convert_shouldCreateJwtAuthenticationToken() {
        
        Jwt jwt = createJwtWithAuthorities(List.of("ROLE_USER"));

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        assertThat(auth.getClass().getSimpleName()).isEqualTo("JwtAuthenticationToken");
    }

    @Test
    void convert_shouldPreserveOriginalJwt() {
        
        Jwt jwt = createJwtWithAuthorities(List.of("ROLE_USER"));

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        assertThat(auth.getCredentials()).isEqualTo(jwt);
    }

    @Test
    void convert_shouldHandleCustomAuthorities() {
        
        List<String> customAuthorities = List.of("read:users", "write:users", "delete:users");
        Jwt jwt = createJwtWithAuthorities(customAuthorities);

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).hasSize(3);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("read:users", "write:users", "delete:users");
    }

    @Test
    void convert_shouldHandleSpecialCharactersInAuthorities() {
        
        List<String> authorities = List.of("ROLE_USER", "scope:api:read", "permission:admin.write");
        Jwt jwt = createJwtWithAuthorities(authorities);

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).hasSize(3);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "scope:api:read", "permission:admin.write");
    }

    @Test
    void convert_shouldReturnUniqueAuthorities() {
        
        Jwt jwt = createJwtWithAuthorities(List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_USER", "ROLE_ADMIN"));

        
        AbstractAuthenticationToken auth = converter.convert(jwt);

        
        
        assertThat(auth).isNotNull();
        Collection<? extends GrantedAuthority> grantedAuthorities = auth.getAuthorities();
        assertThat(grantedAuthorities).hasSize(2);
        assertThat(grantedAuthorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    private Jwt createJwtWithAuthorities(List<String> authorities) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "testuser",
                        "authorities", authorities
                )
        );
    }

    private Jwt createJwtWithSubject(String subject, List<String> authorities) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", subject,
                        "authorities", authorities
                )
        );
    }

    private Jwt createJwtWithoutAuthorities() {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("sub", "testuser")
        );
    }

    private Jwt createJwtWithMixedAuthorities() {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "testuser",
                        "authorities", List.of("ROLE_USER", 123, "ROLE_ADMIN", true)
                )
        );
    }

    private Jwt createJwtWithNullAuthorities() {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("sub", "testuser");
        claims.put("authorities", null);
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims
        );
    }
}
