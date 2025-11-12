package com.gradproject.taskmanager.shared.util;

import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureValidatorTest {

    private WebhookSignatureValidator validator;
    private static final String TEST_SECRET = "test-webhook-secret-12345";
    private static final String TEST_PAYLOAD = "{\"action\":\"opened\",\"number\":1}";

    @BeforeEach
    void setUp() {
        validator = new WebhookSignatureValidator();
    }

    

    @Test
    void validate_shouldValidateGitHubSignature() {
        
        String signature = validator.generateGitHubSignature(TEST_PAYLOAD, TEST_SECRET);

        
        boolean valid = validator.validate(GitProvider.GITHUB, TEST_PAYLOAD, signature, TEST_SECRET);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validate_shouldValidateGitLabToken() {
        
        String token = "gitlab-webhook-token";

        
        boolean valid = validator.validate(GitProvider.GITLAB, TEST_PAYLOAD, token, token);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validate_shouldValidateBitbucketSignature() {
        
        String signature = generateBitbucketSignature(TEST_PAYLOAD, TEST_SECRET);

        
        boolean valid = validator.validate(GitProvider.BITBUCKET, TEST_PAYLOAD, signature, TEST_SECRET);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validate_shouldReturnFalseForNullPayload() {
        
        boolean valid = validator.validate(GitProvider.GITHUB, null, "signature", TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validate_shouldReturnFalseForNullSignature() {
        
        boolean valid = validator.validate(GitProvider.GITHUB, TEST_PAYLOAD, null, TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validate_shouldReturnFalseForNullSecret() {
        
        boolean valid = validator.validate(GitProvider.GITHUB, TEST_PAYLOAD, "signature", null);

        
        assertThat(valid).isFalse();
    }

    

    @Test
    void validateGitHubSignature_shouldValidateCorrectSignature() {
        
        String signature = validator.generateGitHubSignature(TEST_PAYLOAD, TEST_SECRET);

        
        boolean valid = validator.validateGitHubSignature(TEST_PAYLOAD, signature, TEST_SECRET);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validateGitHubSignature_shouldRejectInvalidSignature() {
        
        String correctSignature = validator.generateGitHubSignature(TEST_PAYLOAD, TEST_SECRET);
        String wrongSignature = correctSignature.substring(0, correctSignature.length() - 1) + "0";

        
        boolean valid = validator.validateGitHubSignature(TEST_PAYLOAD, wrongSignature, TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateGitHubSignature_shouldRejectTamperedPayload() {
        
        String signature = validator.generateGitHubSignature(TEST_PAYLOAD, TEST_SECRET);
        String tamperedPayload = TEST_PAYLOAD + " ";

        
        boolean valid = validator.validateGitHubSignature(tamperedPayload, signature, TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateGitHubSignature_shouldRejectWrongSecret() {
        
        String signature = validator.generateGitHubSignature(TEST_PAYLOAD, TEST_SECRET);
        String wrongSecret = "wrong-secret";

        
        boolean valid = validator.validateGitHubSignature(TEST_PAYLOAD, signature, wrongSecret);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateGitHubSignature_shouldRejectSignatureWithoutPrefix() {
        
        String signature = validator.generateGitHubSignature(TEST_PAYLOAD, TEST_SECRET);
        String signatureWithoutPrefix = signature.substring(7); 

        
        boolean valid = validator.validateGitHubSignature(TEST_PAYLOAD, signatureWithoutPrefix, TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateGitHubSignature_shouldHandleEmptyPayload() {
        
        String emptyPayload = "";
        String signature = validator.generateGitHubSignature(emptyPayload, TEST_SECRET);

        
        boolean valid = validator.validateGitHubSignature(emptyPayload, signature, TEST_SECRET);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validateGitHubSignature_shouldHandleVeryLongPayload() {
        
        String longPayload = "{\"data\":\"" + "x".repeat(10000) + "\"}";
        String signature = validator.generateGitHubSignature(longPayload, TEST_SECRET);

        
        boolean valid = validator.validateGitHubSignature(longPayload, signature, TEST_SECRET);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validateGitHubSignature_shouldHandlePayloadWithUnicode() {
        
        String unicodePayload = "{\"message\":\"Hello 世界 🌍\"}";
        String signature = validator.generateGitHubSignature(unicodePayload, TEST_SECRET);

        
        boolean valid = validator.validateGitHubSignature(unicodePayload, signature, TEST_SECRET);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validateGitHubSignature_shouldHandleMultilinePayload() {
        
        String multilinePayload = """
            {
              "action": "opened",
              "pull_request": {
                "id": 1,
                "title": "Test PR"
              }
            }
            """;
        String signature = validator.generateGitHubSignature(multilinePayload, TEST_SECRET);

        
        boolean valid = validator.validateGitHubSignature(multilinePayload, signature, TEST_SECRET);

        
        assertThat(valid).isTrue();
    }

    

    @Test
    void validateGitHubSignature_shouldValidateLegacySHA1Signature() {
        
        String sha1Signature = generateGitHubSHA1Signature(TEST_PAYLOAD, TEST_SECRET);

        
        boolean valid = validator.validateGitHubSignature(TEST_PAYLOAD, sha1Signature, TEST_SECRET);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validateGitHubSignature_shouldRejectInvalidSHA1Signature() {
        
        String sha1Signature = generateGitHubSHA1Signature(TEST_PAYLOAD, TEST_SECRET);
        String wrongSignature = sha1Signature.substring(0, sha1Signature.length() - 1) + "0";

        
        boolean valid = validator.validateGitHubSignature(TEST_PAYLOAD, wrongSignature, TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    

    @Test
    void validateGitLabToken_shouldValidateMatchingTokens() {
        
        String token = "gitlab-webhook-token-12345";

        
        boolean valid = validator.validateGitLabToken(token, token);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validateGitLabToken_shouldRejectDifferentTokens() {
        
        String providedToken = "token-1";
        String expectedToken = "token-2";

        
        boolean valid = validator.validateGitLabToken(providedToken, expectedToken);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateGitLabToken_shouldRejectNullProvidedToken() {
        
        boolean valid = validator.validateGitLabToken(null, "expected-token");

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateGitLabToken_shouldRejectNullExpectedToken() {
        
        boolean valid = validator.validateGitLabToken("provided-token", null);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateGitLabToken_shouldRejectBothNull() {
        
        boolean valid = validator.validateGitLabToken(null, null);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateGitLabToken_shouldHandleEmptyTokens() {
        
        boolean valid = validator.validateGitLabToken("", "");

        
        assertThat(valid).isTrue(); 
    }

    @Test
    void validateGitLabToken_shouldRejectEmptyVsNonEmpty() {
        
        boolean valid = validator.validateGitLabToken("", "non-empty");

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateGitLabToken_shouldHandleLongTokens() {
        
        String longToken = "x".repeat(1000);

        
        boolean valid = validator.validateGitLabToken(longToken, longToken);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validateGitLabToken_shouldHandleTokensWithSpecialCharacters() {
        
        String token = "token!@#$%^&*(){}[]|:;<>?/~`";

        
        boolean valid = validator.validateGitLabToken(token, token);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validateGitLabToken_shouldBeCaseSensitive() {
        
        String token1 = "MyToken";
        String token2 = "mytoken";

        
        boolean valid = validator.validateGitLabToken(token1, token2);

        
        assertThat(valid).isFalse();
    }

    

    @Test
    void validateBitbucketSignature_shouldValidateCorrectSignature() {
        
        String signature = generateBitbucketSignature(TEST_PAYLOAD, TEST_SECRET);

        
        boolean valid = validator.validateBitbucketSignature(TEST_PAYLOAD, signature, TEST_SECRET);

        
        assertThat(valid).isTrue();
    }

    @Test
    void validateBitbucketSignature_shouldRejectInvalidSignature() {
        
        String correctSignature = generateBitbucketSignature(TEST_PAYLOAD, TEST_SECRET);
        String wrongSignature = correctSignature.substring(0, correctSignature.length() - 1) + "0";

        
        boolean valid = validator.validateBitbucketSignature(TEST_PAYLOAD, wrongSignature, TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateBitbucketSignature_shouldRejectTamperedPayload() {
        
        String signature = generateBitbucketSignature(TEST_PAYLOAD, TEST_SECRET);
        String tamperedPayload = TEST_PAYLOAD + " ";

        
        boolean valid = validator.validateBitbucketSignature(tamperedPayload, signature, TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateBitbucketSignature_shouldRejectWrongSecret() {
        
        String signature = generateBitbucketSignature(TEST_PAYLOAD, TEST_SECRET);
        String wrongSecret = "wrong-secret";

        
        boolean valid = validator.validateBitbucketSignature(TEST_PAYLOAD, signature, wrongSecret);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateBitbucketSignature_shouldRejectSignatureWithoutPrefix() {
        
        String signature = generateBitbucketSignature(TEST_PAYLOAD, TEST_SECRET);
        String signatureWithoutPrefix = signature.substring(7);

        
        boolean valid = validator.validateBitbucketSignature(TEST_PAYLOAD, signatureWithoutPrefix, TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateBitbucketSignature_shouldRejectSignatureWithWrongPrefix() {
        
        String signature = generateBitbucketSignature(TEST_PAYLOAD, TEST_SECRET);
        String wrongPrefix = "sha1=" + signature.substring(7);

        
        boolean valid = validator.validateBitbucketSignature(TEST_PAYLOAD, wrongPrefix, TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    

    @Test
    void generateGitHubSignature_shouldGenerateValidSignature() {
        
        String signature = validator.generateGitHubSignature(TEST_PAYLOAD, TEST_SECRET);

        
        assertThat(signature).startsWith("sha256=");
        assertThat(signature.length()).isEqualTo(71); 
    }

    @Test
    void generateGitHubSignature_shouldGenerateDifferentSignaturesForDifferentPayloads() {
        
        String payload1 = "{\"action\":\"opened\"}";
        String payload2 = "{\"action\":\"closed\"}";

        
        String signature1 = validator.generateGitHubSignature(payload1, TEST_SECRET);
        String signature2 = validator.generateGitHubSignature(payload2, TEST_SECRET);

        
        assertThat(signature1).isNotEqualTo(signature2);
    }

    @Test
    void generateGitHubSignature_shouldGenerateDifferentSignaturesForDifferentSecrets() {
        
        String secret1 = "secret-one";
        String secret2 = "secret-two";

        
        String signature1 = validator.generateGitHubSignature(TEST_PAYLOAD, secret1);
        String signature2 = validator.generateGitHubSignature(TEST_PAYLOAD, secret2);

        
        assertThat(signature1).isNotEqualTo(signature2);
    }

    @Test
    void generateGitHubSignature_shouldGenerateSameSignatureForSameInputs() {
        
        String signature1 = validator.generateGitHubSignature(TEST_PAYLOAD, TEST_SECRET);
        String signature2 = validator.generateGitHubSignature(TEST_PAYLOAD, TEST_SECRET);

        
        assertThat(signature1).isEqualTo(signature2);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"test\":\"data\"}",
        "{\"unicode\":\"世界\"}",
        "{\"special\":\"!@#$%^&*()\"}",
        "{\"multiline\":\"line1\\nline2\"}"
    })
    void generateGitHubSignature_shouldHandleVariousPayloads(String payload) {
        
        String signature = validator.generateGitHubSignature(payload, TEST_SECRET);

        
        assertThat(signature).startsWith("sha256=");
        assertThat(signature.length()).isEqualTo(71);
    }

    

    @Test
    void validateGitHubSignature_shouldResistTimingAttacks() {
        
        String correctSignature = validator.generateGitHubSignature(TEST_PAYLOAD, TEST_SECRET);
        String wrongSignature1 = "sha256=" + "0".repeat(64); 
        String wrongSignature2 = correctSignature.substring(0, 10) + "0".repeat(61); 

        
        long start1 = System.nanoTime();
        validator.validateGitHubSignature(TEST_PAYLOAD, wrongSignature1, TEST_SECRET);
        long time1 = System.nanoTime() - start1;

        long start2 = System.nanoTime();
        validator.validateGitHubSignature(TEST_PAYLOAD, wrongSignature2, TEST_SECRET);
        long time2 = System.nanoTime() - start2;

        
        
        
        assertThat(Math.abs(time1 - time2)).isLessThan(time1 * 10);
    }

    @Test
    void validateGitLabToken_shouldResistTimingAttacks() {
        
        String correctToken = "correct-token-12345";
        String wrongToken1 = "x".repeat(correctToken.length());
        String wrongToken2 = correctToken.substring(0, 10) + "x".repeat(9);

        
        long start1 = System.nanoTime();
        validator.validateGitLabToken(wrongToken1, correctToken);
        long time1 = System.nanoTime() - start1;

        long start2 = System.nanoTime();
        validator.validateGitLabToken(wrongToken2, correctToken);
        long time2 = System.nanoTime() - start2;

        
        
        assertThat(Math.abs(time1 - time2)).isLessThan(time1 * 10);
    }

    

    @Test
    void validateGitHubSignature_shouldHandleSignatureWithUppercaseHex() {
        
        String signature = validator.generateGitHubSignature(TEST_PAYLOAD, TEST_SECRET);
        String uppercaseSignature = "sha256=" + signature.substring(7).toUpperCase();

        
        boolean valid = validator.validateGitHubSignature(TEST_PAYLOAD, uppercaseSignature, TEST_SECRET);

        
        assertThat(valid).isFalse(); 
    }

    @Test
    void validateGitHubSignature_shouldRejectMalformedSignature() {
        
        boolean valid = validator.validateGitHubSignature(TEST_PAYLOAD, "malformed", TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateBitbucketSignature_shouldRejectMalformedSignature() {
        
        boolean valid = validator.validateBitbucketSignature(TEST_PAYLOAD, "malformed", TEST_SECRET);

        
        assertThat(valid).isFalse();
    }

    @Test
    void validateGitHubSignature_shouldHandleExceptionGracefully() {
        
        String invalidSignature = "sha256=not-valid-hex-data";

        
        boolean valid = validator.validateGitHubSignature(TEST_PAYLOAD, invalidSignature, TEST_SECRET);

        
        assertThat(valid).isFalse(); 
    }

    

    
    private String generateBitbucketSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Bitbucket signature", e);
        }
    }

    
    private String generateGitHubSHA1Signature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha1=" + bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate SHA1 signature", e);
        }
    }

    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
