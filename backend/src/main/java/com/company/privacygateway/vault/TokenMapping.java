package com.company.privacygateway.vault;

import java.time.Instant;
import java.util.UUID;

public record TokenMapping(
        UUID id,
        String tenantId,
        String conversationId,
        String tokenId,
        String entityType,
        String encryptedValue,
        String valueHash,
        Instant createdAt,
        Instant expiresAt,
        String createdBy,
        String status
) {
}
