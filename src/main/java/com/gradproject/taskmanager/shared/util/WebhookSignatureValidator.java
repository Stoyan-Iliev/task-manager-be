package com.gradproject.taskmanager.shared.util;

import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;


@Component
public class WebhookSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HMAC_SHA1 = "HmacSHA1";

    
    public boolean validate(GitProvider provider, String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            return false;
        }

        return switch (provider) {
            case GITHUB -> validateGitHubSignature(payload, signature, secret);
            case GITLAB -> validateGitLabToken(signature, secret);
            case BITBUCKET -> validateBitbucketSignature(payload, signature, secret);
        };
    }

    
    public boolean validateGitHubSignature(String payload, String signature, String secret) {
        try {
            
            if (!signature.startsWith("sha256=")) {
                
                if (signature.startsWith("sha1=")) {
                    return validateGitHubSignatureSHA1(payload, signature, secret);
                }
                return false;
            }

            String expectedSignature = signature.substring(7); 

            
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = bytesToHex(hash);

            
            return constantTimeEquals(expectedSignature, computedSignature);

        } catch (Exception e) {
            return false;
        }
    }

    
    private boolean validateGitHubSignatureSHA1(String payload, String signature, String secret) {
        try {
            String expectedSignature = signature.substring(5); 

            Mac mac = Mac.getInstance(HMAC_SHA1);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA1);
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = bytesToHex(hash);

            return constantTimeEquals(expectedSignature, computedSignature);

        } catch (Exception e) {
            return false;
        }
    }

    
    public boolean validateGitLabToken(String providedToken, String expectedToken) {
        if (providedToken == null || expectedToken == null) {
            return false;
        }

        
        return constantTimeEquals(providedToken, expectedToken);
    }

    
    public boolean validateBitbucketSignature(String payload, String signature, String secret) {
        try {
            
            if (!signature.startsWith("sha256=")) {
                return false;
            }

            String expectedSignature = signature.substring(7);

            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = bytesToHex(hash);

            return constantTimeEquals(expectedSignature, computedSignature);

        } catch (Exception e) {
            return false;
        }
    }

    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(aBytes, bBytes);
    }

    
    public String generateGitHubSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + bytesToHex(hash);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
}
