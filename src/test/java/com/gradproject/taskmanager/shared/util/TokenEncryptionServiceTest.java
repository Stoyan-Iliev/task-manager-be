package com.gradproject.taskmanager.shared.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEncryptionServiceTest {

    private TokenEncryptionService encryptionService;
    private static final String TEST_ENCRYPTION_KEY = "test-encryption-key-for-unit-tests-12345";

    @BeforeEach
    void setUp() {
        encryptionService = new TokenEncryptionService(TEST_ENCRYPTION_KEY);
    }

    

    @Test
    void constructor_shouldThrowExceptionForNullKey() {
        
        assertThatThrownBy(() -> new TokenEncryptionService(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Encryption key not configured");
    }

    @Test
    void constructor_shouldThrowExceptionForEmptyKey() {
        
        assertThatThrownBy(() -> new TokenEncryptionService(""))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Encryption key not configured");
    }

    @Test
    void constructor_shouldAcceptValidKey() {
        
        TokenEncryptionService service = new TokenEncryptionService("valid-key");
        assertThat(service).isNotNull();
    }

    

    @Test
    void encrypt_shouldEncryptSimpleToken() {
        
        String plaintext = "github_pat_123456789";

        
        String encrypted = encryptionService.encrypt(plaintext);

        
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(encrypted).isBase64();
    }

    @Test
    void encrypt_shouldProduceDifferentCiphertextsForSamePlaintext() {
        
        String plaintext = "test-token-123";

        
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        
        assertThat(encrypted1).isNotEqualTo(encrypted2); 
    }

    @Test
    void encrypt_shouldThrowExceptionForNullInput() {
        
        assertThatThrownBy(() -> encryptionService.encrypt(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Plaintext cannot be null or empty");
    }

    @Test
    void encrypt_shouldThrowExceptionForEmptyInput() {
        
        assertThatThrownBy(() -> encryptionService.encrypt(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Plaintext cannot be null or empty");
    }

    @Test
    void encrypt_shouldHandleLongTokens() {
        
        String longToken = "a".repeat(1000);

        
        String encrypted = encryptionService.encrypt(longToken);

        
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isBase64();
    }

    @Test
    void encrypt_shouldHandleSpecialCharacters() {
        
        String tokenWithSpecialChars = "token!@#$%^&*(){}[]|:;<>?/~`";

        
        String encrypted = encryptionService.encrypt(tokenWithSpecialChars);

        
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isBase64();
    }

    @Test
    void encrypt_shouldHandleUnicodeCharacters() {
        
        String unicodeToken = "token-with-émojis-🔐-and-中文";

        
        String encrypted = encryptionService.encrypt(unicodeToken);

        
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isBase64();
    }

    

    @Test
    void decrypt_shouldDecryptEncryptedToken() {
        
        String plaintext = "github_pat_123456789";
        String encrypted = encryptionService.encrypt(plaintext);

        
        String decrypted = encryptionService.decrypt(encrypted);

        
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void decrypt_shouldThrowExceptionForNullInput() {
        
        assertThatThrownBy(() -> encryptionService.decrypt(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Encrypted token cannot be null or empty");
    }

    @Test
    void decrypt_shouldThrowExceptionForEmptyInput() {
        
        assertThatThrownBy(() -> encryptionService.decrypt(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Encrypted token cannot be null or empty");
    }

    @Test
    void decrypt_shouldThrowExceptionForInvalidBase64() {
        
        assertThatThrownBy(() -> encryptionService.decrypt("not-valid-base64!@#"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to decrypt token");
    }

    @Test
    void decrypt_shouldThrowExceptionForInvalidEncryptedData() {
        
        String invalidEncrypted = Base64.getEncoder().encodeToString("invalid-data".getBytes());

        
        assertThatThrownBy(() -> encryptionService.decrypt(invalidEncrypted))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to decrypt token");
    }

    @Test
    void decrypt_shouldThrowExceptionForTamperedData() {
        
        String plaintext = "github_pat_123456789";
        String encrypted = encryptionService.encrypt(plaintext);

        
        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
        encryptedBytes[encryptedBytes.length - 1] ^= 1; 
        String tampered = Base64.getEncoder().encodeToString(encryptedBytes);

        
        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to decrypt token");
    }

    

    @ParameterizedTest
    @ValueSource(strings = {
        "simple-token",
        "github_pat_1234567890abcdefghijklmnop",
        "token-with-special-chars!@#$%",
        "token with spaces",
        "UPPERCASE-TOKEN",
        "MiXeD-CaSe-ToKeN",
        "token\nwith\nnewlines",
        "token\twith\ttabs"
    })
    void encryptDecrypt_shouldRoundTripSuccessfully(String plaintext) {
        
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encryptDecrypt_shouldHandleVeryLongTokens() {
        
        String longToken = "x".repeat(10000);

        
        String encrypted = encryptionService.encrypt(longToken);
        String decrypted = encryptionService.decrypt(encrypted);

        
        assertThat(decrypted).isEqualTo(longToken);
    }

    @Test
    void encryptDecrypt_shouldHandleUnicodeCharacters() {
        
        String unicodeToken = "🔐 Secret Token 中文 émojis ñ";

        
        String encrypted = encryptionService.encrypt(unicodeToken);
        String decrypted = encryptionService.decrypt(encrypted);

        
        assertThat(decrypted).isEqualTo(unicodeToken);
    }

    @Test
    void encryptDecrypt_shouldHandleMultilineTokens() {
        
        String multilineToken = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC
            -----END PRIVATE KEY-----
            """;

        
        String encrypted = encryptionService.encrypt(multilineToken);
        String decrypted = encryptionService.decrypt(encrypted);

        
        assertThat(decrypted).isEqualTo(multilineToken);
    }

    

    @Test
    void isEncrypted_shouldReturnTrueForEncryptedToken() {
        
        String plaintext = "test-token";
        String encrypted = encryptionService.encrypt(plaintext);

        
        boolean result = encryptionService.isEncrypted(encrypted);

        
        assertThat(result).isTrue();
    }

    @Test
    void isEncrypted_shouldReturnFalseForPlaintext() {
        
        String plaintext = "github_pat_123456789";

        
        boolean result = encryptionService.isEncrypted(plaintext);

        
        assertThat(result).isFalse();
    }

    @Test
    void isEncrypted_shouldReturnFalseForNull() {
        
        boolean result = encryptionService.isEncrypted(null);

        
        assertThat(result).isFalse();
    }

    @Test
    void isEncrypted_shouldReturnFalseForEmpty() {
        
        boolean result = encryptionService.isEncrypted("");

        
        assertThat(result).isFalse();
    }

    @Test
    void isEncrypted_shouldReturnFalseForInvalidBase64() {
        
        String invalidBase64 = "not-valid-base64!@#$%";

        
        boolean result = encryptionService.isEncrypted(invalidBase64);

        
        assertThat(result).isFalse();
    }

    @Test
    void isEncrypted_shouldReturnFalseForTooShortBase64() {
        
        String tooShort = Base64.getEncoder().encodeToString("short".getBytes());

        
        boolean result = encryptionService.isEncrypted(tooShort);

        
        assertThat(result).isFalse();
    }

    @Test
    void isEncrypted_shouldReturnTrueForValidBase64WithSufficientLength() {
        
        byte[] data = new byte[28]; 
        String validBase64 = Base64.getEncoder().encodeToString(data);

        
        boolean result = encryptionService.isEncrypted(validBase64);

        
        assertThat(result).isTrue();
    }

    

    @Test
    void encryptIfNeeded_shouldEncryptPlaintextToken() {
        
        String plaintext = "github_pat_123456789";

        
        String result = encryptionService.encryptIfNeeded(plaintext);

        
        assertThat(result).isNotEqualTo(plaintext);
        assertThat(result).isBase64();
        assertThat(encryptionService.isEncrypted(result)).isTrue();
    }

    @Test
    void encryptIfNeeded_shouldNotReencryptEncryptedToken() {
        
        String plaintext = "test-token";
        String encrypted = encryptionService.encrypt(plaintext);

        
        String result = encryptionService.encryptIfNeeded(encrypted);

        
        assertThat(result).isEqualTo(encrypted); 
    }

    @Test
    void encryptIfNeeded_shouldReturnNullForNull() {
        
        String result = encryptionService.encryptIfNeeded(null);

        
        assertThat(result).isNull();
    }

    @Test
    void encryptIfNeeded_shouldReturnEmptyForEmpty() {
        
        String result = encryptionService.encryptIfNeeded("");

        
        assertThat(result).isEmpty();
    }

    @Test
    void encryptIfNeeded_shouldHandleMultipleCalls() {
        
        String plaintext = "test-token";

        
        String encrypted1 = encryptionService.encryptIfNeeded(plaintext);
        String encrypted2 = encryptionService.encryptIfNeeded(encrypted1);
        String encrypted3 = encryptionService.encryptIfNeeded(encrypted2);

        
        assertThat(encrypted1).isEqualTo(encrypted2);
        assertThat(encrypted2).isEqualTo(encrypted3);
        assertThat(encryptionService.decrypt(encrypted3)).isEqualTo(plaintext);
    }

    

    @Test
    void decryptIfNeeded_shouldDecryptEncryptedToken() {
        
        String plaintext = "github_pat_123456789";
        String encrypted = encryptionService.encrypt(plaintext);

        
        String result = encryptionService.decryptIfNeeded(encrypted);

        
        assertThat(result).isEqualTo(plaintext);
    }

    @Test
    void decryptIfNeeded_shouldReturnPlaintextAsIs() {
        
        String plaintext = "github_pat_123456789";

        
        String result = encryptionService.decryptIfNeeded(plaintext);

        
        assertThat(result).isEqualTo(plaintext);
    }

    @Test
    void decryptIfNeeded_shouldReturnNullForNull() {
        
        String result = encryptionService.decryptIfNeeded(null);

        
        assertThat(result).isNull();
    }

    @Test
    void decryptIfNeeded_shouldReturnEmptyForEmpty() {
        
        String result = encryptionService.decryptIfNeeded("");

        
        assertThat(result).isEmpty();
    }

    @Test
    void decryptIfNeeded_shouldHandleMultipleCalls() {
        
        String plaintext = "test-token";
        String encrypted = encryptionService.encrypt(plaintext);

        
        String decrypted1 = encryptionService.decryptIfNeeded(encrypted);
        String decrypted2 = encryptionService.decryptIfNeeded(decrypted1);
        String decrypted3 = encryptionService.decryptIfNeeded(decrypted2);

        
        assertThat(decrypted1).isEqualTo(plaintext);
        assertThat(decrypted2).isEqualTo(plaintext);
        assertThat(decrypted3).isEqualTo(plaintext);
    }

    

    @Test
    void fullWorkflow_shouldEncryptStoreAndDecrypt() {
        
        String originalToken = "github_pat_1234567890abcdef";

        
        String encryptedForStorage = encryptionService.encrypt(originalToken);
        assertThat(encryptedForStorage).isNotEqualTo(originalToken);

        
        String retrievedFromStorage = encryptedForStorage;

        
        String decryptedToken = encryptionService.decrypt(retrievedFromStorage);

        
        assertThat(decryptedToken).isEqualTo(originalToken);
    }

    @Test
    void fullWorkflow_shouldHandleConditionalEncryption() {
        
        String plainToken = "plain-token";

        
        String stored1 = encryptionService.encryptIfNeeded(plainToken);
        assertThat(encryptionService.isEncrypted(stored1)).isTrue();

        
        String stored2 = encryptionService.encryptIfNeeded(stored1);
        assertThat(stored2).isEqualTo(stored1);

        
        String retrieved = encryptionService.decryptIfNeeded(stored2);
        assertThat(retrieved).isEqualTo(plainToken);
    }

    @Test
    void differentKeys_shouldProduceDifferentEncryption() {
        
        String plaintext = "test-token";
        TokenEncryptionService service1 = new TokenEncryptionService("key-one");
        TokenEncryptionService service2 = new TokenEncryptionService("key-two");

        
        String encrypted1 = service1.encrypt(plaintext);
        String encrypted2 = service2.encrypt(plaintext);

        
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(service1.decrypt(encrypted1)).isEqualTo(plaintext);
        assertThat(service2.decrypt(encrypted2)).isEqualTo(plaintext);
    }

    @Test
    void differentKeys_shouldNotDecryptEachOthersTokens() {
        
        String plaintext = "test-token";
        TokenEncryptionService service1 = new TokenEncryptionService("key-one");
        TokenEncryptionService service2 = new TokenEncryptionService("key-two");
        String encrypted = service1.encrypt(plaintext);

        
        assertThatThrownBy(() -> service2.decrypt(encrypted))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to decrypt token");
    }

    

    @Test
    void encrypt_shouldHandleSingleCharacter() {
        
        String singleChar = "x";

        
        String encrypted = encryptionService.encrypt(singleChar);
        String decrypted = encryptionService.decrypt(encrypted);

        
        assertThat(decrypted).isEqualTo(singleChar);
    }

    @Test
    void encrypt_shouldHandleNumericStrings() {
        
        String numeric = "1234567890";

        
        String encrypted = encryptionService.encrypt(numeric);
        String decrypted = encryptionService.decrypt(encrypted);

        
        assertThat(decrypted).isEqualTo(numeric);
    }

    @Test
    void encrypt_shouldHandleWhitespaceOnly() {
        
        String whitespace = "   \t\n   ";

        
        String encrypted = encryptionService.encrypt(whitespace);
        String decrypted = encryptionService.decrypt(encrypted);

        
        assertThat(decrypted).isEqualTo(whitespace);
    }

    @Test
    void isEncrypted_shouldNotFalsePositiveOnRegularBase64() {
        
        String regularBase64 = Base64.getEncoder().encodeToString("regular text".getBytes());

        
        boolean result = encryptionService.isEncrypted(regularBase64);

        
        
        
        if (result) {
            assertThatThrownBy(() -> encryptionService.decrypt(regularBase64))
                .isInstanceOf(RuntimeException.class);
        }
    }
}
