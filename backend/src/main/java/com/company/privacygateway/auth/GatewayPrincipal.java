package com.company.privacygateway.auth;

/**
 * Authenticated caller identity from JWT claims (D-006).
 */
public record GatewayPrincipal(String userId, String tenantId, boolean reidentify) {
}
