package com.company.privacygateway.vault;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;

public final class HmacValueHasher implements ValueHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] keyBytes;

    public HmacValueHasher(String base64Key) {
        this.keyBytes = decodeKey(base64Key);
    }

    @Override
    public String hash(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext must not be null");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(keyBytes, ALGORITHM));
            byte[] digest = mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("value hash failed", ex);
        }
    }

    private static byte[] decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("VAULT_HASH_KEY or VAULT_ENCRYPTION_KEY is required for hashing");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            if (keyBytes.length < 16) {
                throw new IllegalStateException("hash key must decode to at least 16 bytes");
            }
            return keyBytes;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("hash key must be Base64-encoded", ex);
        }
    }
}
