package com.company.privacygateway.conversation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;

@Repository
public class ConversationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public ConversationRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public void ensureExists(String tenantId, String conversationId, String createdBy) {
        Instant now = clock.instant();
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM conversation
                WHERE tenant_id = ? AND id = ?
                """,
                Integer.class,
                tenantId,
                conversationId);
        if (count != null && count > 0) {
            jdbcTemplate.update(
                    """
                    UPDATE conversation
                    SET updated_at = ?
                    WHERE tenant_id = ? AND id = ?
                    """,
                    Timestamp.from(now),
                    tenantId,
                    conversationId);
            return;
        }
        jdbcTemplate.update(
                """
                INSERT INTO conversation (id, tenant_id, created_by, created_at, updated_at, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                conversationId,
                tenantId,
                createdBy,
                Timestamp.from(now),
                Timestamp.from(now),
                "ACTIVE");
    }
}
