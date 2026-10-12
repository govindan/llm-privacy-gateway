package com.company.privacygateway.privacy.policy;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;

@Service
public class DefaultPolicyEngine implements PolicyEngine {

    @Override
    public PolicyDecision evaluate(SensitiveEntity entity, PrivacyPolicy policy) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }

        Optional<PolicyRule> matched = policy.rules().stream()
                .filter(rule -> rule.entityType().equalsIgnoreCase(entity.type()))
                .max(Comparator.comparingInt(PolicyRule::priority));

        if (matched.isEmpty()) {
            // D-008: never ALLOW by omission
            return new PolicyDecision(entity.type(), PolicyAction.REDACT, false, null);
        }

        PolicyRule rule = matched.get();
        return new PolicyDecision(entity.type(), rule.action(), true, rule.priority());
    }
}
