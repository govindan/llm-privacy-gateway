package com.company.privacygateway.privacy.policy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcPolicyRepository implements PolicyRepository {

    public static final String STATUS_ACTIVE = "ACTIVE";

    private static final RowMapper<PrivacyPolicy> POLICY_MAPPER = (rs, rowNum) -> new PrivacyPolicy(
            UUID.fromString(rs.getString("id")),
            rs.getString("tenant_id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("version"),
            rs.getString("status"),
            List.of());

    private final JdbcTemplate jdbcTemplate;

    public JdbcPolicyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PrivacyPolicy> findActivePolicy(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }

        List<PrivacyPolicy> policies = jdbcTemplate.query(
                """
                SELECT id, tenant_id, name, description, version, status
                FROM policy
                WHERE tenant_id = ?
                  AND status = ?
                ORDER BY version DESC
                """,
                POLICY_MAPPER,
                tenantId,
                STATUS_ACTIVE);

        if (policies.isEmpty()) {
            return Optional.empty();
        }

        PrivacyPolicy header = policies.getFirst();
        List<PolicyRule> rules = jdbcTemplate.query(
                """
                SELECT id, entity_type, action, priority
                FROM policy_rule
                WHERE policy_id = ?
                ORDER BY priority DESC, entity_type ASC
                """,
                (rs, rowNum) -> new PolicyRule(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("entity_type"),
                        PolicyAction.fromStorage(rs.getString("action")),
                        rs.getInt("priority")),
                header.id());

        return Optional.of(new PrivacyPolicy(
                header.id(),
                header.tenantId(),
                header.name(),
                header.description(),
                header.version(),
                header.status(),
                rules));
    }
}
