package com.gradproject.taskmanager.infrastructure.security.jwt;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.ArrayList;
import java.util.List;

public class CompositeJwtDecoder implements JwtDecoder {

    private final List<JwtDecoder> delegates = new ArrayList<>();

    public CompositeJwtDecoder(List<JwtDecoder> decoders) {
        this.delegates.addAll(decoders);
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        JwtException last = null;
        for (JwtDecoder d : delegates) {
            try {
                return d.decode(token);
            } catch (JwtException ex) {
                last = ex;
            }
        }
        if (last != null) throw last;
        throw new JwtException("No decoders available");
    }
}
