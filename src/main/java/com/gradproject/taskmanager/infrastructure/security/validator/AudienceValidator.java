package com.gradproject.taskmanager.infrastructure.security.validator;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> allowedAudiences;

    public AudienceValidator(List<String> allowedAudiences) {
        this.allowedAudiences = allowedAudiences;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Collection<String> aud = token.getAudience();
        if (aud == null || aud.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing audience", null));
        }
        boolean match = aud.stream().anyMatch(allowedAudiences::contains);
        if (!match) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
