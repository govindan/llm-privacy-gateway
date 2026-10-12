package com.company.privacygateway.privacy.policy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPolicyEngineTest {

    private final PolicyEngine engine = new DefaultPolicyEngine();

    @Test
    void evaluatesAllSupportedActions() {
        PrivacyPolicy policy = policyWith(
                rule("EMAIL", PolicyAction.ALLOW, 10),
                rule("PERSON", PolicyAction.TOKENIZE, 10),
                rule("PHONE", PolicyAction.MASK, 10),
                rule("CARD_NUMBER", PolicyAction.REDACT, 10),
                rule("GOVERNMENT_ID", PolicyAction.DENY, 10));

        assertThat(engine.evaluate(new SensitiveEntity("EMAIL"), policy).action())
                .isEqualTo(PolicyAction.ALLOW);
        assertThat(engine.evaluate(new SensitiveEntity("PERSON"), policy).action())
                .isEqualTo(PolicyAction.TOKENIZE);
        assertThat(engine.evaluate(new SensitiveEntity("PHONE"), policy).action())
                .isEqualTo(PolicyAction.MASK);
        assertThat(engine.evaluate(new SensitiveEntity("CARD_NUMBER"), policy).action())
                .isEqualTo(PolicyAction.REDACT);
        assertThat(engine.evaluate(new SensitiveEntity("GOVERNMENT_ID"), policy).action())
                .isEqualTo(PolicyAction.DENY);
    }

    @Test
    void unmatchedEntityTypeDefaultsToRedact() {
        PrivacyPolicy policy = policyWith(rule("EMAIL", PolicyAction.TOKENIZE, 10));

        PolicyDecision decision = engine.evaluate(new SensitiveEntity("IP_ADDRESS"), policy);

        assertThat(decision.matchedRule()).isFalse();
        assertThat(decision.action()).isEqualTo(PolicyAction.REDACT);
    }

    @Test
    void higherPriorityRuleWins() {
        PrivacyPolicy policy = policyWith(
                rule("EMAIL", PolicyAction.ALLOW, 1),
                rule("EMAIL", PolicyAction.DENY, 50));

        // Unique constraint in DB prevents duplicate entity types, but engine must still
        // choose max priority if multiple rules are supplied in-memory.
        assertThat(engine.evaluate(new SensitiveEntity("EMAIL"), policy).action())
                .isEqualTo(PolicyAction.DENY);
        assertThat(engine.evaluate(new SensitiveEntity("EMAIL"), policy).rulePriority())
                .isEqualTo(50);
    }

    private static PrivacyPolicy policyWith(PolicyRule... rules) {
        return new PrivacyPolicy(
                UUID.randomUUID(),
                "tenant-test",
                "TEST",
                "test",
                1,
                "ACTIVE",
                List.of(rules));
    }

    private static PolicyRule rule(String type, PolicyAction action, int priority) {
        return new PolicyRule(UUID.randomUUID(), type, action, priority);
    }
}
