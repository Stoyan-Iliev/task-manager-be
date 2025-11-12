package com.gradproject.taskmanager.shared.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;


@Service
public class TokenEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; 
    private static final int GCM_TAG_LENGTH = 128; 

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public TokenEncryptionService(@Value("${security.encryption.key:}") String encryptionKey) {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            throw new IllegalStateException(
                "Encryption key not configured. Set 'security.encryption.key' in application properties."
            );
        }

        
        this.secretKey = deriveKey(encryptionKey);
        this.secureRandom = new SecureRandom();
    }

    
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }

        try {
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt token", e);
        }
    }

    
    public String decrypt(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isEmpty()) {
            throw new IllegalArgumentException("Encrypted token cannot be null or empty");
        }

        try {
            
            byte[] combined = Base64.getDecoder().decode(encryptedToken);

            
            ByteBuffer byteBuffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt token", e);
        }
    }

    
    public boolean isEncrypted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(token);
            
            return decoded.length >= GCM_IV_LENGTH + 16;
        } catch (IllegalArgumentException e) {
            
            return false;
        }
    }

    
    private SecretKey deriveKey(String encryptionKey) {
        try {
            
            
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }

    
    public String encryptIfNeeded(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }

        return isEncrypted(token) ? token : encrypt(token);
    }

    
    public String decryptIfNeeded(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }

        return isEncrypted(token) ? decrypt(token) : token;
    }
}
