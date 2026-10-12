package com.company.privacygateway.privacy.policy;

import java.util.List;
import java.util.UUID;

public record PrivacyPolicy(
        UUID id,
        String tenantId,
        String name,
        String description,
        int version,
        String status,
        List<PolicyRule> rules
) {
    public PrivacyPolicy {
        rules = List.copyOf(rules);
    }
}
