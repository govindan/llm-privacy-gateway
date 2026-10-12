package com.company.privacygateway.vault;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmEncryptionServiceTest {

    private static final String KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Test
    void encryptDecryptRoundTrip() {
        EncryptionService encryption = new AesGcmEncryptionService(KEY);
        String plaintext = "john.smith@example.com";

        String ciphertext = encryption.encrypt(plaintext);

        assertThat(ciphertext).isNotBlank().doesNotContain(plaintext);
        assertThat(encryption.decrypt(ciphertext)).isEqualTo(plaintext);
    }

    @Test
    void encryptProducesDifferentCiphertexts() {
        EncryptionService encryption = new AesGcmEncryptionService(KEY);
        String first = encryption.encrypt("same-value");
        String second = encryption.encrypt("same-value");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void invalidCiphertextIsRejected() {
        EncryptionService encryption = new AesGcmEncryptionService(KEY);
        assertThatThrownBy(() -> encryption.decrypt("not-valid-base64!!!"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> encryption.decrypt("YWJj"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingKeyFailsClosed() {
        assertThatThrownBy(() -> new AesGcmEncryptionService(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VAULT_ENCRYPTION_KEY");
    }

    @Test
    void wrongKeyLengthFailsClosed() {
        String shortKey = java.util.Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new AesGcmEncryptionService(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
