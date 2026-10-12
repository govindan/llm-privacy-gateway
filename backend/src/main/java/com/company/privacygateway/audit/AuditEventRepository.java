package com.company.privacygateway.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class AuditEventRepository {

    private static final RowMapper<AuditEvent> ROW_MAPPER = (rs, rowNum) -> new AuditEvent(
            UUID.fromString(rs.getString("id")),
            rs.getString("tenant_id"),
            rs.getString("conversation_id"),
            rs.getTimestamp("event_timestamp").toInstant(),
            rs.getString("user_id"),
            rs.getString("operation"),
            rs.getString("policy_id") == null ? null : UUID.fromString(rs.getString("policy_id")),
            rs.getString("entity_type"),
            rs.getString("action"),
            rs.getBoolean("success"),
            rs.getString("metadata"),
            rs.getString("correlation_id"));

    private final JdbcTemplate jdbcTemplate;

    public AuditEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(AuditEvent event) {
        jdbcTemplate.update(
                """
                INSERT INTO audit_event (
                    id, tenant_id, conversation_id, event_timestamp, user_id, operation,
                    policy_id, entity_type, action, success, metadata, correlation_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                event.id(),
                event.tenantId(),
                event.conversationId(),
                Timestamp.from(event.timestamp()),
                event.userId(),
                event.operation(),
                event.policyId(),
                event.entityType(),
                event.action(),
                event.success(),
                event.metadata(),
                event.correlationId());
    }

    public List<AuditEvent> findByTenantAndConversation(String tenantId, String conversationId) {
        return jdbcTemplate.query(
                """
                SELECT id, tenant_id, conversation_id, event_timestamp, user_id, operation,
                       policy_id, entity_type, action, success, metadata, correlation_id
                FROM audit_event
                WHERE tenant_id = ? AND conversation_id = ?
                ORDER BY event_timestamp ASC
                """,
                ROW_MAPPER,
                tenantId,
                conversationId);
    }
}
