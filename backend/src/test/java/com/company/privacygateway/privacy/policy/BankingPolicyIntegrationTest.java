package com.company.privacygateway.privacy.policy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BankingPolicyIntegrationTest {

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyEngine policyEngine;

    @Test
    void seededBankingStandardPolicyMatchesSampleActions() {
        PrivacyPolicy policy = policyRepository
                .findActivePolicy("demo-tenant")
                .orElseThrow(() -> new AssertionError("BANKING_STANDARD seed missing"));

        assertThat(policy.name()).isEqualTo("BANKING_STANDARD");
        assertThat(policy.status()).isEqualTo("ACTIVE");

        Map<String, PolicyAction> actions = policy.rules().stream()
                .collect(Collectors.toMap(PolicyRule::entityType, PolicyRule::action));

        assertThat(actions)
                .containsEntry("PERSON", PolicyAction.TOKENIZE)
                .containsEntry("EMAIL", PolicyAction.TOKENIZE)
                .containsEntry("PHONE", PolicyAction.TOKENIZE)
                .containsEntry("ACCOUNT_NUMBER", PolicyAction.TOKENIZE)
                .containsEntry("CARD_NUMBER", PolicyAction.REDACT)
                .containsEntry("IBAN", PolicyAction.TOKENIZE)
                .containsEntry("ADDRESS", PolicyAction.TOKENIZE)
                .containsEntry("DATE_OF_BIRTH", PolicyAction.TOKENIZE)
                .containsEntry("DATE", PolicyAction.TOKENIZE)
                .containsEntry("CUSTOMER_ID", PolicyAction.TOKENIZE)
                .containsEntry("TRANSACTION_ID", PolicyAction.TOKENIZE)
                .containsEntry("AMOUNT", PolicyAction.ALLOW);

        assertThat(policyEngine.evaluate(new SensitiveEntity("AMOUNT"), policy).action())
                .isEqualTo(PolicyAction.ALLOW);
        assertThat(policyEngine.evaluate(new SensitiveEntity("CARD_NUMBER"), policy).action())
                .isEqualTo(PolicyAction.REDACT);
        assertThat(policyEngine.evaluate(new SensitiveEntity("IP_ADDRESS"), policy).action())
                .isEqualTo(PolicyAction.REDACT);
    }
}
