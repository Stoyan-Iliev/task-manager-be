package com.gradproject.taskmanager.infrastructure.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositeJwtDecoderTest {

    @Mock
    private JwtDecoder decoder1;

    @Mock
    private JwtDecoder decoder2;

    @Mock
    private JwtDecoder decoder3;

    private CompositeJwtDecoder compositeDecoder;

    @BeforeEach
    void setUp() {
        compositeDecoder = new CompositeJwtDecoder(List.of(decoder1, decoder2, decoder3));
    }

    @Test
    void decode_shouldReturnTokenFromFirstSuccessfulDecoder() {
        
        String token = "valid.jwt.token";
        Jwt expectedJwt = createMockJwt();

        when(decoder1.decode(token)).thenReturn(expectedJwt);

        
        Jwt result = compositeDecoder.decode(token);

        
        assertThat(result).isEqualTo(expectedJwt);
        verify(decoder1).decode(token);
        verify(decoder2, never()).decode(anyString());
        verify(decoder3, never()).decode(anyString());
    }

    @Test
    void decode_shouldTryNextDecoderWhenFirstFails() {
        
        String token = "valid.jwt.token";
        Jwt expectedJwt = createMockJwt();

        when(decoder1.decode(token)).thenThrow(new JwtException("Decoder 1 failed"));
        when(decoder2.decode(token)).thenReturn(expectedJwt);

        
        Jwt result = compositeDecoder.decode(token);

        
        assertThat(result).isEqualTo(expectedJwt);
        verify(decoder1).decode(token);
        verify(decoder2).decode(token);
        verify(decoder3, never()).decode(anyString());
    }

    @Test
    void decode_shouldTryAllDecodersUntilSuccess() {
        
        String token = "valid.jwt.token";
        Jwt expectedJwt = createMockJwt();

        when(decoder1.decode(token)).thenThrow(new JwtException("Decoder 1 failed"));
        when(decoder2.decode(token)).thenThrow(new JwtException("Decoder 2 failed"));
        when(decoder3.decode(token)).thenReturn(expectedJwt);

        
        Jwt result = compositeDecoder.decode(token);

        
        assertThat(result).isEqualTo(expectedJwt);
        verify(decoder1).decode(token);
        verify(decoder2).decode(token);
        verify(decoder3).decode(token);
    }

    @Test
    void decode_shouldThrowLastExceptionWhenAllDecodersFail() {
        
        String token = "invalid.jwt.token";
        JwtException lastException = new JwtException("Decoder 3 failed - signature invalid");

        when(decoder1.decode(token)).thenThrow(new JwtException("Decoder 1 failed"));
        when(decoder2.decode(token)).thenThrow(new JwtException("Decoder 2 failed"));
        when(decoder3.decode(token)).thenThrow(lastException);

        
        assertThatThrownBy(() -> compositeDecoder.decode(token))
                .isInstanceOf(JwtException.class)
                .hasMessage("Decoder 3 failed - signature invalid");

        verify(decoder1).decode(token);
        verify(decoder2).decode(token);
        verify(decoder3).decode(token);
    }

    @Test
    void decode_shouldThrowExceptionWhenNoDecodersAvailable() {
        
        CompositeJwtDecoder emptyComposite = new CompositeJwtDecoder(List.of());
        String token = "any.jwt.token";

        
        assertThatThrownBy(() -> emptyComposite.decode(token))
                .isInstanceOf(JwtException.class)
                .hasMessage("No decoders available");
    }

    @Test
    void decode_shouldHandleSingleDecoder() {
        
        CompositeJwtDecoder singleDecoder = new CompositeJwtDecoder(List.of(decoder1));
        String token = "valid.jwt.token";
        Jwt expectedJwt = createMockJwt();

        when(decoder1.decode(token)).thenReturn(expectedJwt);

        
        Jwt result = singleDecoder.decode(token);

        
        assertThat(result).isEqualTo(expectedJwt);
        verify(decoder1).decode(token);
    }

    @Test
    void decode_shouldHandleSingleDecoderFailure() {
        
        CompositeJwtDecoder singleDecoder = new CompositeJwtDecoder(List.of(decoder1));
        String token = "invalid.jwt.token";
        JwtException exception = new JwtException("Single decoder failed");

        when(decoder1.decode(token)).thenThrow(exception);

        
        assertThatThrownBy(() -> singleDecoder.decode(token))
                .isInstanceOf(JwtException.class)
                .hasMessage("Single decoder failed");

        verify(decoder1).decode(token);
    }

    @Test
    void decode_shouldPreserveOriginalExceptionType() {
        
        String token = "expired.jwt.token";
        JwtException expiredException = new JwtException("Token expired");

        when(decoder1.decode(token)).thenThrow(expiredException);

        CompositeJwtDecoder singleDecoder = new CompositeJwtDecoder(List.of(decoder1));

        
        assertThatThrownBy(() -> singleDecoder.decode(token))
                .isSameAs(expiredException);
    }

    @Test
    void decode_shouldStopAtFirstSuccessEvenWithMultipleValidDecoders() {
        
        String token = "valid.jwt.token";
        Jwt jwt1 = createMockJwt();

        when(decoder1.decode(token)).thenReturn(jwt1);

        
        Jwt result = compositeDecoder.decode(token);

        
        assertThat(result).isEqualTo(jwt1);
        verify(decoder1).decode(token);
        verify(decoder2, never()).decode(anyString());
        verify(decoder3, never()).decode(anyString());
    }

    private Jwt createMockJwt() {
        return new Jwt(
                "mock.jwt.token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("sub", "testuser")
        );
    }
}
