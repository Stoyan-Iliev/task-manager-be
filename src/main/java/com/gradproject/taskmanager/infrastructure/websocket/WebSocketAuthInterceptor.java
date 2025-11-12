package com.gradproject.taskmanager.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            
            List<String> authHeaders = accessor.getNativeHeader("Authorization");

            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);

                    
                    if (token.isEmpty()) {
                        log.debug("WebSocket CONNECT with empty Bearer token");
                        return message;
                    }

                    try {
                        
                        Jwt jwt = jwtDecoder.decode(token);

                        if (jwt == null) {
                            log.warn("JWT decoder returned null for token");
                            return message;
                        }

                        String username = jwt.getSubject();

                        
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        new ArrayList<>() 
                                );

                        
                        accessor.setUser(authentication);

                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("WebSocket connection authenticated for user: {}", username);
                    } catch (JwtException e) {
                        log.warn("Invalid JWT token in WebSocket connection: {}", e.getMessage());
                        
                        
                    }
                } else {
                    log.debug("WebSocket CONNECT without valid Bearer token");
                }
            } else {
                log.debug("WebSocket CONNECT without Authorization header");
            }
        }

        return message;
    }
}
