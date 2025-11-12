package com.gradproject.taskmanager.infrastructure.config;

import com.gradproject.taskmanager.infrastructure.websocket.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final CorsProperties corsProperties;

    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        
        
        
        registry.enableSimpleBroker("/topic", "/user");

        
        registry.setApplicationDestinationPrefixes("/app");

        
        registry.setUserDestinationPrefix("/user");

        log.info("Configured message broker with /topic and /user destinations");
    }

    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        
        String[] allowedOrigins = corsProperties.getAllowedOrigins().toArray(new String[0]);

        
        
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();

        log.info("Registered WebSocket endpoint /ws with SockJS support");
        log.info("Allowed origin patterns for WebSocket: {}", (Object) allowedOrigins);
    }

    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
        log.info("Added authentication interceptor to client inbound channel");
    }
}
