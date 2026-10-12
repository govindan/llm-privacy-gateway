package com.company.privacygateway.vault;

import com.company.privacygateway.configuration.AppProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcTokenVault implements TokenVault {

    static final String STATUS_ACTIVE = "ACTIVE";

    private static final RowMapper<TokenMapping> ROW_MAPPER = (rs, rowNum) -> new TokenMapping(
            UUID.fromString(rs.getString("id")),
            rs.getString("tenant_id"),
            rs.getString("conversation_id"),
            rs.getString("token_id"),
            rs.getString("entity_type"),
            rs.getString("encrypted_value"),
            rs.getString("value_hash"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("expires_at").toInstant(),
            rs.getString("created_by"),
            rs.getString("status"));

    private final JdbcTemplate jdbcTemplate;
    private final EncryptionService encryptionService;
    private final ValueHasher valueHasher;
    private final AppProperties appProperties;
    private final Clock clock;

    public JdbcTokenVault(
            JdbcTemplate jdbcTemplate,
            EncryptionService encryptionService,
            ValueHasher valueHasher,
            AppProperties appProperties,
            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionService = encryptionService;
        this.valueHasher = valueHasher;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TokenMapping createOrGet(
            String tenantId,
            String conversationId,
            String entityType,
            String plaintext,
            String createdBy) {
        requireNonBlank(tenantId, "tenantId");
        requireNonBlank(conversationId, "conversationId");
        requireNonBlank(entityType, "entityType");
        requireNonBlank(plaintext, "plaintext");
        requireNonBlank(createdBy, "createdBy");

        String valueHash = valueHasher.hash(plaintext);
        Optional<TokenMapping> existing = findActiveByHash(tenantId, conversationId, entityType, valueHash);
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(appProperties.getTokenTtlHours() * 3600L);
        String encryptedValue = encryptionService.encrypt(plaintext);
        if (encryptedValue.contains(plaintext)) {
            throw new IllegalStateException("encrypted value must not contain plaintext");
        }

        Optional<TokenMapping> prior = findAnyByHash(tenantId, conversationId, entityType, valueHash);
        if (prior.isPresent()) {
            return renewMapping(prior.get(), encryptedValue, createdBy, now, expiresAt);
        }

        for (int attempt = 0; attempt < 5; attempt++) {
            String tokenId = OpaqueTokenIds.formatToken(entityType, OpaqueTokenIds.randomHexId());
            UUID id = UUID.randomUUID();
            try {
                jdbcTemplate.update(
                        """
                        INSERT INTO token_mapping (
                            id, tenant_id, conversation_id, token_id, entity_type,
                            encrypted_value, value_hash, created_at, expires_at, created_by, status
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        id,
                        tenantId,
                        conversationId,
                        tokenId,
                        entityType,
                        encryptedValue,
                        valueHash,
                        Timestamp.from(now),
                        Timestamp.from(expiresAt),
                        createdBy,
                        STATUS_ACTIVE);
                return new TokenMapping(
                        id,
                        tenantId,
                        conversationId,
                        tokenId,
                        entityType,
                        encryptedValue,
                        valueHash,
                        now,
                        expiresAt,
                        createdBy,
                        STATUS_ACTIVE);
            } catch (DuplicateKeyException ex) {
                Optional<TokenMapping> raced = findActiveByHash(tenantId, conversationId, entityType, valueHash);
                if (raced.isPresent()) {
                    return raced.get();
                }
                Optional<TokenMapping> racedAny = findAnyByHash(tenantId, conversationId, entityType, valueHash);
                if (racedAny.isPresent()) {
                    return renewMapping(racedAny.get(), encryptedValue, createdBy, now, expiresAt);
                }
            }
        }
        throw new IllegalStateException("unable to allocate opaque token id");
    }

    private TokenMapping renewMapping(
            TokenMapping prior,
            String encryptedValue,
            String createdBy,
            Instant now,
            Instant expiresAt) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String tokenId = OpaqueTokenIds.formatToken(prior.entityType(), OpaqueTokenIds.randomHexId());
            try {
                int updated = jdbcTemplate.update(
                        """
                        UPDATE token_mapping
                        SET token_id = ?,
                            encrypted_value = ?,
                            created_at = ?,
                            expires_at = ?,
                            created_by = ?,
                            status = ?
                        WHERE id = ?
                        """,
                        tokenId,
                        encryptedValue,
                        Timestamp.from(now),
                        Timestamp.from(expiresAt),
                        createdBy,
                        STATUS_ACTIVE,
                        prior.id());
                if (updated != 1) {
                    throw new IllegalStateException("token mapping renew failed");
                }
                return new TokenMapping(
                        prior.id(),
                        prior.tenantId(),
                        prior.conversationId(),
                        tokenId,
                        prior.entityType(),
                        encryptedValue,
                        prior.valueHash(),
                        now,
                        expiresAt,
                        createdBy,
                        STATUS_ACTIVE);
            } catch (DuplicateKeyException ignored) {
                // rare token_id collision; retry with a new opaque id
            }
        }
        throw new IllegalStateException("unable to allocate opaque token id");
    }

    @Override
    public Optional<TokenMapping> resolve(String tenantId, String conversationId, String tokenId) {
        requireNonBlank(tenantId, "tenantId");
        requireNonBlank(conversationId, "conversationId");
        requireNonBlank(tokenId, "tokenId");

        Instant now = clock.instant();
        List<TokenMapping> rows = jdbcTemplate.query(
                """
                SELECT id, tenant_id, conversation_id, token_id, entity_type,
                       encrypted_value, value_hash, created_at, expires_at, created_by, status
                FROM token_mapping
                WHERE tenant_id = ?
                  AND conversation_id = ?
                  AND token_id = ?
                  AND status = ?
                  AND expires_at > ?
                """,
                ROW_MAPPER,
                tenantId,
                conversationId,
                tokenId,
                STATUS_ACTIVE,
                Timestamp.from(now));
        return rows.stream().findFirst();
    }

    @Override
    public String decryptValue(TokenMapping mapping) {
        if (mapping == null) {
            throw new IllegalArgumentException("mapping must not be null");
        }
        return encryptionService.decrypt(mapping.encryptedValue());
    }

    private Optional<TokenMapping> findActiveByHash(
            String tenantId, String conversationId, String entityType, String valueHash) {
        Instant now = clock.instant();
        List<TokenMapping> rows = jdbcTemplate.query(
                """
                SELECT id, tenant_id, conversation_id, token_id, entity_type,
                       encrypted_value, value_hash, created_at, expires_at, created_by, status
                FROM token_mapping
                WHERE tenant_id = ?
                  AND conversation_id = ?
                  AND entity_type = ?
                  AND value_hash = ?
                  AND status = ?
                  AND expires_at > ?
                """,
                ROW_MAPPER,
                tenantId,
                conversationId,
                entityType,
                valueHash,
                STATUS_ACTIVE,
                Timestamp.from(now));
        return rows.stream().findFirst();
    }

    private Optional<TokenMapping> findAnyByHash(
            String tenantId, String conversationId, String entityType, String valueHash) {
        List<TokenMapping> rows = jdbcTemplate.query(
                """
                SELECT id, tenant_id, conversation_id, token_id, entity_type,
                       encrypted_value, value_hash, created_at, expires_at, created_by, status
                FROM token_mapping
                WHERE tenant_id = ?
                  AND conversation_id = ?
                  AND entity_type = ?
                  AND value_hash = ?
                """,
                ROW_MAPPER,
                tenantId,
                conversationId,
                entityType,
                valueHash);
        return rows.stream().findFirst();
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
