package com.gradproject.taskmanager.infrastructure.security;

import com.gradproject.taskmanager.infrastructure.config.SecurityJwtProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyManagerTest {

    @Mock
    private SecurityJwtProperties props;

    @Mock
    private SecurityJwtProperties.KeyProperties keyProps;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private Environment environment;

    @Test
    void constructor_shouldGenerateEphemeralKeysInDevMode() {
        
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(props.getKey()).thenReturn(keyProps);
        when(keyProps.getPemPublic()).thenReturn(null);
        when(keyProps.getPemPrivate()).thenReturn(null);

        
        KeyManager keyManager = new KeyManager(props, resourceLoader, environment);

        
        assertThat(keyManager.keysLoaded()).isTrue();
        assertThat(keyManager.getCurrentKid()).isNotNull();
    }

    @Test
    void constructor_shouldGenerateEphemeralKeysInDockerMode() {
        
        when(environment.getActiveProfiles()).thenReturn(new String[]{"docker"});
        when(props.getKey()).thenReturn(keyProps);
        when(keyProps.getPemPublic()).thenReturn(null);
        when(keyProps.getPemPrivate()).thenReturn(null);

        
        KeyManager keyManager = new KeyManager(props, resourceLoader, environment);

        
        assertThat(keyManager.keysLoaded()).isTrue();
        assertThat(keyManager.getCurrentKid()).isNotNull();
    }

    @Test
    void constructor_shouldThrowExceptionWhenNoKeysInProdMode() {
        
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(props.getKey()).thenReturn(keyProps);
        when(keyProps.getPemPublic()).thenReturn(null);
        when(keyProps.getPemPrivate()).thenReturn(null);

        
        assertThatThrownBy(() -> new KeyManager(props, resourceLoader, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No JWT keys configured for prod environment");
    }

    @Test
    void keysLoaded_shouldReturnTrueWhenKeysExist() {
        
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(props.getKey()).thenReturn(keyProps);
        when(keyProps.getPemPublic()).thenReturn(null);
        when(keyProps.getPemPrivate()).thenReturn(null);

        KeyManager keyManager = new KeyManager(props, resourceLoader, environment);

        
        boolean loaded = keyManager.keysLoaded();

        
        assertThat(loaded).isTrue();
    }

    @Test
    void getCurrentKid_shouldReturnNonNullValue() {
        
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(props.getKey()).thenReturn(keyProps);
        when(keyProps.getPemPublic()).thenReturn(null);
        when(keyProps.getPemPrivate()).thenReturn(null);

        KeyManager keyManager = new KeyManager(props, resourceLoader, environment);

        
        String kid = keyManager.getCurrentKid();

        
        assertThat(kid).isNotNull();
        assertThat(kid).isNotEmpty();
    }

    @Test
    void jwkSource_shouldReturnNonNullJWKSource() {
        
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(props.getKey()).thenReturn(keyProps);
        when(keyProps.getPemPublic()).thenReturn(null);
        when(keyProps.getPemPrivate()).thenReturn(null);

        KeyManager keyManager = new KeyManager(props, resourceLoader, environment);

        
        JWKSource<?> jwkSource = keyManager.jwkSource();

        
        assertThat(jwkSource).isNotNull();
    }

    @Test
    void jwkSet_shouldReturnNonNullJWKSet() {
        
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(props.getKey()).thenReturn(keyProps);
        when(keyProps.getPemPublic()).thenReturn(null);
        when(keyProps.getPemPrivate()).thenReturn(null);

        KeyManager keyManager = new KeyManager(props, resourceLoader, environment);

        
        JWKSet jwkSet = keyManager.jwkSet();

        
        assertThat(jwkSet).isNotNull();
        assertThat(jwkSet.getKeys()).isNotEmpty();
    }

    @Test
    void publicKeys_shouldReturnNonEmptyList() {
        
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(props.getKey()).thenReturn(keyProps);
        when(keyProps.getPemPublic()).thenReturn(null);
        when(keyProps.getPemPrivate()).thenReturn(null);

        KeyManager keyManager = new KeyManager(props, resourceLoader, environment);

        
        List<RSAPublicKey> publicKeys = keyManager.publicKeys();

        
        assertThat(publicKeys).isNotNull();
        assertThat(publicKeys).isNotEmpty();
        assertThat(publicKeys.getFirst()).isInstanceOf(RSAPublicKey.class);
    }

    @Test
    void ephemeralKeys_shouldBeValid2048BitRSAKeys() {
        
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(props.getKey()).thenReturn(keyProps);
        when(keyProps.getPemPublic()).thenReturn(null);
        when(keyProps.getPemPrivate()).thenReturn(null);

        KeyManager keyManager = new KeyManager(props, resourceLoader, environment);

        
        List<RSAPublicKey> publicKeys = keyManager.publicKeys();

        
        assertThat(publicKeys).hasSize(1);
        RSAPublicKey publicKey = publicKeys.getFirst();
        
        assertThat(publicKey.getModulus().bitLength()).isEqualTo(2048);
    }

    @Test
    void jwkSet_shouldContainKeyWithKid() {
        
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(props.getKey()).thenReturn(keyProps);
        when(keyProps.getPemPublic()).thenReturn(null);
        when(keyProps.getPemPrivate()).thenReturn(null);

        KeyManager keyManager = new KeyManager(props, resourceLoader, environment);

        
        JWKSet jwkSet = keyManager.jwkSet();

        
        assertThat(jwkSet.getKeys()).hasSize(1);
        assertThat(jwkSet.getKeys().getFirst().getKeyID()).isNotNull();
        assertThat(jwkSet.getKeys().getFirst().getKeyID()).isEqualTo(keyManager.getCurrentKid());
    }

    @Test
    void multipleInstances_shouldGenerateDifferentKeys() {
        
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(props.getKey()).thenReturn(keyProps);
        when(keyProps.getPemPublic()).thenReturn(null);
        when(keyProps.getPemPrivate()).thenReturn(null);

        
        KeyManager keyManager1 = new KeyManager(props, resourceLoader, environment);
        KeyManager keyManager2 = new KeyManager(props, resourceLoader, environment);

        
        
        assertThat(keyManager1.getCurrentKid()).isNotEqualTo(keyManager2.getCurrentKid());
        assertThat(keyManager1.publicKeys().getFirst()).isNotEqualTo(keyManager2.publicKeys().getFirst());
    }
}
