package com.gradproject.taskmanager.infrastructure.security.validator;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTest {

    @Test
    void validate_shouldSucceedWhenAudienceMatches() {
        
        AudienceValidator validator = new AudienceValidator(List.of("task-manager-api"));
        Jwt jwt = createJwt(List.of("task-manager-api"));

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_shouldSucceedWhenOneOfMultipleAudiencesMatches() {
        
        AudienceValidator validator = new AudienceValidator(List.of("task-manager-api", "admin-api"));
        Jwt jwt = createJwt(List.of("task-manager-api", "other-api"));

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_shouldSucceedWhenTokenHasMultipleAudiencesAndOneMatches() {
        
        AudienceValidator validator = new AudienceValidator(List.of("expected-api"));
        Jwt jwt = createJwt(List.of("other-api", "expected-api", "another-api"));

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_shouldFailWhenAudienceDoesNotMatch() {
        
        AudienceValidator validator = new AudienceValidator(List.of("task-manager-api"));
        Jwt jwt = createJwt(List.of("other-api"));

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_token");
        assertThat(result.getErrors().iterator().next().getDescription()).isEqualTo("Invalid audience");
    }

    @Test
    void validate_shouldFailWhenTokenAudienceIsNull() {
        
        AudienceValidator validator = new AudienceValidator(List.of("task-manager-api"));
        Jwt jwt = createJwtWithoutAudience();

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_token");
        assertThat(result.getErrors().iterator().next().getDescription()).isEqualTo("Missing audience");
    }

    @Test
    void validate_shouldFailWhenTokenAudienceIsEmpty() {
        
        AudienceValidator validator = new AudienceValidator(List.of("task-manager-api"));
        Jwt jwt = createJwt(List.of());

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_token");
        assertThat(result.getErrors().iterator().next().getDescription()).isEqualTo("Missing audience");
    }

    @Test
    void validate_shouldFailWhenNoAudiencesMatch() {
        
        AudienceValidator validator = new AudienceValidator(List.of("expected-api-1", "expected-api-2"));
        Jwt jwt = createJwt(List.of("other-api-1", "other-api-2"));

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().iterator().next().getDescription()).isEqualTo("Invalid audience");
    }

    @Test
    void validate_shouldSucceedWithMultipleAllowedAudiences() {
        
        AudienceValidator validator = new AudienceValidator(List.of("api-1", "api-2", "api-3"));
        Jwt jwt = createJwt(List.of("api-2"));

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_shouldBeCaseSensitive() {
        
        AudienceValidator validator = new AudienceValidator(List.of("task-manager-api"));
        Jwt jwt = createJwt(List.of("TASK-MANAGER-API"));

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void validate_shouldHandleExactMatch() {
        
        String expectedAudience = "exact-match-api";
        AudienceValidator validator = new AudienceValidator(List.of(expectedAudience));
        Jwt jwt = createJwt(List.of(expectedAudience));

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_shouldNotMatchSubstrings() {
        
        AudienceValidator validator = new AudienceValidator(List.of("api"));
        Jwt jwt = createJwt(List.of("my-api"));

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void validate_shouldHandleSpecialCharactersInAudience() {
        
        String specialAudience = "https://api.example.com/v1";
        AudienceValidator validator = new AudienceValidator(List.of(specialAudience));
        Jwt jwt = createJwt(List.of(specialAudience));

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_shouldHandleEmptyAllowedAudiences() {
        
        AudienceValidator validator = new AudienceValidator(List.of());
        Jwt jwt = createJwt(List.of("any-api"));

        
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        
        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt createJwt(List<String> audiences) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "testuser",
                        "aud", audiences
                )
        );
    }

    private Jwt createJwtWithoutAudience() {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("sub", "testuser")
        );
    }
}
