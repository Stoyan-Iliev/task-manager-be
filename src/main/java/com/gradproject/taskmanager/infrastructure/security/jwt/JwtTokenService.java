package com.gradproject.taskmanager.infrastructure.security.jwt;

import com.gradproject.taskmanager.infrastructure.config.SecurityJwtProperties;
import com.gradproject.taskmanager.infrastructure.security.KeyManager;
import com.gradproject.taskmanager.modules.auth.domain.Role;
import com.gradproject.taskmanager.modules.auth.domain.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityJwtProperties props;
    private final KeyManager keyManager;

    public JwtTokenService(JwtEncoder jwtEncoder, SecurityJwtProperties props, KeyManager keyManager) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.keyManager = keyManager;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessTokenTtl());
        String jti = UUID.randomUUID().toString();

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        List<String> authorities = roles; 

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.getIssuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(user.getId().toString())
                .audience(props.getAudience())
                .id(jti)
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim("authorities", authorities)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(SignatureAlgorithm.RS256)
                .type("JWT")
                .keyId(keyManager.getCurrentKid())
                .build();

        JwtEncoderParameters params = JwtEncoderParameters.from(jwsHeader, claims);
        return jwtEncoder.encode(params).getTokenValue();
    }

    public long getAccessTokenExpiresInSeconds() {
        return props.getAccessTokenTtl().toSeconds();
    }
}
