package com.company.privacygateway.audit;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Records security-relevant operations without plaintext protected values (D-018).
 */
@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final Clock clock;

    public AuditService(AuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(
            String tenantId,
            String conversationId,
            String userId,
            String operation,
            UUID policyId,
            String entityType,
            String action,
            boolean success,
            String metadata,
            String correlationId) {
        if (metadata != null && looksLikeSensitive(metadata)) {
            throw new IllegalArgumentException("audit metadata must not contain sensitive values");
        }
        repository.insert(new AuditEvent(
                UUID.randomUUID(),
                tenantId,
                conversationId,
                clock.instant(),
                userId,
                operation,
                policyId,
                entityType,
                action,
                success,
                metadata,
                correlationId));
    }

    public List<AuditEvent> listForConversation(String tenantId, String conversationId) {
        return repository.findByTenantAndConversation(tenantId, conversationId);
    }

    private static boolean looksLikeSensitive(String metadata) {
        String lower = metadata.toLowerCase();
        return lower.contains("@")
                || lower.contains("password")
                || lower.contains("bearer ")
                || metadata.contains("<<");
    }
}
