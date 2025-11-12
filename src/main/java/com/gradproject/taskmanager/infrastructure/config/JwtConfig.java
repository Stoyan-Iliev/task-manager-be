package com.gradproject.taskmanager.infrastructure.config;

import com.gradproject.taskmanager.infrastructure.security.KeyManager;
import com.gradproject.taskmanager.infrastructure.security.jwt.CompositeJwtDecoder;
import com.gradproject.taskmanager.infrastructure.security.validator.AudienceValidator;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class JwtConfig {

    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyManager keyManager) {
        return keyManager.jwkSource();
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyManager keyManager, SecurityJwtProperties props) {
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(props.getIssuer());
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(props.getAudience());
        JwtTimestampValidator ts = new JwtTimestampValidator(props.getClockSkew());
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience, ts);

        List<JwtDecoder> decoders = new ArrayList<>();
        for (RSAPublicKey pk : keyManager.publicKeys()) {
            NimbusJwtDecoder nd = NimbusJwtDecoder.withPublicKey(pk).build();
            nd.setJwtValidator(validator);
            decoders.add(nd);
        }
        if (decoders.isEmpty()) {
            throw new IllegalStateException("No JWT public keys available");
        }
        return decoders.size() == 1 ? decoders.getFirst() : new CompositeJwtDecoder(decoders);
    }
}
