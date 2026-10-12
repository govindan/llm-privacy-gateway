package com.company.privacygateway.privacy.policy;

import java.util.UUID;

public record PolicyRule(
        UUID id,
        String entityType,
        PolicyAction action,
        int priority
) {
}
