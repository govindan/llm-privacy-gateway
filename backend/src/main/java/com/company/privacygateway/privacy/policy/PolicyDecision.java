package com.company.privacygateway.privacy.policy;

public record PolicyDecision(
        String entityType,
        PolicyAction action,
        boolean matchedRule,
        Integer rulePriority
) {
}
