package com.company.privacygateway.vault;

/**
 * Encrypts and decrypts vault values with AES-256-GCM.
 * Ciphertext must never be logged as if it were plaintext.
 */
public interface EncryptionService {

    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
