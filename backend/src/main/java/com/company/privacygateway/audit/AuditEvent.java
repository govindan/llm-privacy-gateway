package com.company.privacygateway.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        String tenantId,
        String conversationId,
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
