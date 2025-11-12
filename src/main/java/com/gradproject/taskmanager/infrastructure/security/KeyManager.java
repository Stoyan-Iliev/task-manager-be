package com.gradproject.taskmanager.infrastructure.security;

import com.gradproject.taskmanager.infrastructure.config.SecurityJwtProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.Getter;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeyManager {

    private final SecurityJwtProperties props;
    private final ResourceLoader resourceLoader;
    private final Environment environment;

    private List<RSAKey> keys = new ArrayList<>();
    @Getter
    private String currentKid;

    public KeyManager(SecurityJwtProperties props, ResourceLoader resourceLoader, Environment environment) {
        this.props = props;
        this.resourceLoader = resourceLoader;
        this.environment = environment;
        loadKeys();
    }

    public synchronized void loadKeys() {
        List<String> active = Arrays.asList(environment.getActiveProfiles());
        boolean prodProfile = active.contains("prod");
        SecurityJwtProperties.KeyProperties kprops = props.getKey();
        List<String> pubList = kprops.getPemPublic();
        List<String> privList = kprops.getPemPrivate();
        List<RSAKey> loaded = new ArrayList<>();
        if (pubList != null && privList != null && !pubList.isEmpty() && pubList.size() == privList.size()) {
            for (int i = 0; i < pubList.size(); i++) {
                RSAKey rsa = loadPemKeys(pubList.get(i), privList.get(i));
                loaded.add(rsa);
            }
        } else {
            
            if (prodProfile) {
                throw new IllegalStateException("No JWT keys configured for prod environment. Configure security.jwt.key.pem.public/private or keystore.");
            }
            loaded.add(generateEphemeral());
        }
        this.keys = loaded;
        
        Set<String> kids = loaded.stream().map(RSAKey::getKeyID).collect(Collectors.toCollection(LinkedHashSet::new));
        if (kprops.getCurrentKid() != null && kids.contains(kprops.getCurrentKid())) {
            this.currentKid = kprops.getCurrentKid();
        } else {
            
            this.currentKid = loaded.getLast().getKeyID();
        }
    }

    private RSAKey loadPemKeys(String pubLocation, String privLocation) {
        try {
            RSAPublicKey publicKey;
            RSAPrivateKey privateKey;
            Resource pubRes = resourceLoader.getResource(pubLocation);
            Resource privRes = resourceLoader.getResource(privLocation);
            try (InputStream in = pubRes.getInputStream()) {
                publicKey = RsaKeyConverters.x509().convert(in);
            }
            try (InputStream in = privRes.getInputStream()) {
                privateKey = RsaKeyConverters.pkcs8().convert(in);
            }
            
            String kid = UUID.randomUUID().toString();
            return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(kid).build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load PEM keys: " + pubLocation + " and " + privLocation, e);
        }
    }

    private RSAKey generateEphemeral() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
            RSAPrivateKey priv = (RSAPrivateKey) kp.getPrivate();
            return new RSAKey.Builder(pub).privateKey(priv).keyID(UUID.randomUUID().toString()).build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to generate RSA keys", e);
        }
    }

    public boolean keysLoaded() {
        return !keys.isEmpty();
    }

    public JWKSource<SecurityContext> jwkSource() {
        JWKSet jwkSet = new JWKSet(new ArrayList<>(keys));
        return new ImmutableJWKSet<>(jwkSet);
    }

    public JWKSet jwkSet() {
        return new JWKSet(new ArrayList<>(keys));
    }

    public java.util.List<java.security.interfaces.RSAPublicKey> publicKeys() {
        java.util.List<java.security.interfaces.RSAPublicKey> pubs = new java.util.ArrayList<>();
        for (RSAKey k : keys) {
            try {
                pubs.add(k.toRSAPublicKey());
            } catch (com.nimbusds.jose.JOSEException e) {
                throw new IllegalStateException("Unable to extract RSAPublicKey", e);
            }
        }
        return pubs;
    }
}
