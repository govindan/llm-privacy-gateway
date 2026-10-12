package com.company.privacygateway.vault;

import java.util.Optional;

public interface TokenVault {

    /**
     * Creates or reuses a tenant- and conversation-scoped mapping for the protected value.
     * The returned mapping never exposes plaintext; encrypted_value is ciphertext only.
     */
    TokenMapping createOrGet(
            String tenantId,
            String conversationId,
            String entityType,
            String plaintext,
            String createdBy);

    /**
     * Resolves an active, non-expired mapping within the tenant and conversation scope.
     */
    Optional<TokenMapping> resolve(String tenantId, String conversationId, String tokenId);

    /**
     * Decrypts a previously resolved mapping. Callers must enforce re-identification authorization.
     */
    String decryptValue(TokenMapping mapping);
}
