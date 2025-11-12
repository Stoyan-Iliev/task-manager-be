package com.gradproject.taskmanager.infrastructure.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    private StompHeaderAccessor accessor;

    @BeforeEach
    void setUp() {
        accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
    }

    

    @Test
    void preSend_withValidJWT_setsAuthentication() {
        
        String validToken = "valid.jwt.token";
        accessor.setNativeHeader("Authorization", "Bearer " + validToken);
        Message<byte[]> message = createMessage(accessor);

        Jwt jwt = createJwt("testuser");
        when(jwtDecoder.decode(validToken)).thenReturn(jwt);

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(resultAccessor).isNotNull();
        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);

        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) resultAccessor.getUser();
        assertThat(auth.getPrincipal()).isEqualTo("testuser");
    }

    @Test
    void preSend_withValidJWT_callsJwtDecoder() {
        
        String validToken = "valid.jwt.token";
        accessor.setNativeHeader("Authorization", "Bearer " + validToken);
        Message<byte[]> message = createMessage(accessor);

        Jwt jwt = createJwt("testuser");
        when(jwtDecoder.decode(validToken)).thenReturn(jwt);

        
        interceptor.preSend(message, messageChannel);

        
        verify(jwtDecoder).decode(validToken);
    }

    

    @Test
    void preSend_withInvalidJWT_doesNotSetAuthentication() {
        
        String invalidToken = "invalid.jwt.token";
        accessor.setNativeHeader("Authorization", "Bearer " + invalidToken);
        Message<byte[]> message = createMessage(accessor);

        when(jwtDecoder.decode(invalidToken)).thenThrow(new JwtException("Invalid token"));

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        assertThat(accessor.getUser()).isNull();
    }

    @Test
    void preSend_withExpiredJWT_doesNotSetAuthentication() {
        
        String expiredToken = "expired.jwt.token";
        accessor.setNativeHeader("Authorization", "Bearer " + expiredToken);
        Message<byte[]> message = createMessage(accessor);

        when(jwtDecoder.decode(expiredToken)).thenThrow(new JwtException("Token expired"));

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        assertThat(accessor.getUser()).isNull();
    }

    

    @Test
    void preSend_withoutAuthorizationHeader_doesNotSetAuthentication() {
        
        Message<byte[]> message = createMessage(accessor);

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        assertThat(accessor.getUser()).isNull();
        verify(jwtDecoder, never()).decode(anyString());
    }

    @Test
    void preSend_withEmptyAuthorizationHeader_doesNotSetAuthentication() {
        
        accessor.setNativeHeader("Authorization", "");
        Message<byte[]> message = createMessage(accessor);

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        assertThat(accessor.getUser()).isNull();
        verify(jwtDecoder, never()).decode(anyString());
    }

    @Test
    void preSend_withoutBearerPrefix_doesNotSetAuthentication() {
        
        accessor.setNativeHeader("Authorization", "valid.jwt.token");
        Message<byte[]> message = createMessage(accessor);

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        assertThat(accessor.getUser()).isNull();
        verify(jwtDecoder, never()).decode(anyString());
    }

    @Test
    void preSend_withOnlyBearerPrefix_doesNotSetAuthentication() {
        
        accessor.setNativeHeader("Authorization", "Bearer ");
        Message<byte[]> message = createMessage(accessor);

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        assertThat(accessor.getUser()).isNull();
        verify(jwtDecoder, never()).decode(anyString());
    }

    

    @Test
    void preSend_withSendCommand_doesNotProcessAuth() {
        
        StompHeaderAccessor sendAccessor = StompHeaderAccessor.create(StompCommand.SEND);
        sendAccessor.setNativeHeader("Authorization", "Bearer valid.token");
        Message<byte[]> message = createMessage(sendAccessor);

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        verify(jwtDecoder, never()).decode(anyString());
    }

    @Test
    void preSend_withSubscribeCommand_doesNotProcessAuth() {
        
        StompHeaderAccessor subscribeAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribeAccessor.setNativeHeader("Authorization", "Bearer valid.token");
        Message<byte[]> message = createMessage(subscribeAccessor);

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        verify(jwtDecoder, never()).decode(anyString());
    }

    @Test
    void preSend_withDisconnectCommand_doesNotProcessAuth() {
        
        StompHeaderAccessor disconnectAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        disconnectAccessor.setNativeHeader("Authorization", "Bearer valid.token");
        Message<byte[]> message = createMessage(disconnectAccessor);

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        verify(jwtDecoder, never()).decode(anyString());
    }

    

    @Test
    void preSend_withNullAccessor_returnsMessage() {
        
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        verify(jwtDecoder, never()).decode(anyString());
    }

    @Test
    void preSend_withMultipleAuthHeaders_usesFirst() {
        
        String validToken = "valid.jwt.token";
        accessor.addNativeHeader("Authorization", "Bearer " + validToken);
        accessor.addNativeHeader("Authorization", "Bearer other.token");
        Message<byte[]> message = createMessage(accessor);

        Jwt jwt = createJwt("testuser");
        when(jwtDecoder.decode(validToken)).thenReturn(jwt);

        
        Message<?> result = interceptor.preSend(message, messageChannel);

        
        assertThat(result).isNotNull();
        verify(jwtDecoder).decode(validToken);
        verify(jwtDecoder, never()).decode("other.token");
    }

    

    private Message<byte[]> createMessage(StompHeaderAccessor accessor) {
        return MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Jwt createJwt(String subject) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("sub", subject, "iat", Instant.now().getEpochSecond())
        );
    }
}
