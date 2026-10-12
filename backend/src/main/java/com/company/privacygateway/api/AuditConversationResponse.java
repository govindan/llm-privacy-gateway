package com.company.privacygateway.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditConversationResponse(String conversationId, List<AuditItem> events) {
    public record AuditItem(
            UUID id,
            Instant timestamp,
            String userId,
            String operation,
            UUID policyId,
            String entityType,
            String action,
            boolean success,
            String metadata,
            String correlationId
    ) {
    }
}
