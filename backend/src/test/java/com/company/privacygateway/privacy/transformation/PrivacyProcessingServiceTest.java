package com.company.privacygateway.privacy.transformation;

import com.company.privacygateway.common.PrivacyProcessingException;
import com.company.privacygateway.privacy.detection.SensitiveDataDetector;
import com.company.privacygateway.privacy.policy.PolicyAction;
import com.company.privacygateway.privacy.policy.PolicyRepository;
import com.company.privacygateway.privacy.policy.PrivacyPolicy;
import com.company.privacygateway.privacy.policy.SensitiveEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class PrivacyProcessingServiceTest {

    private static final String BANKING_TEXT = """
            John Smith, customer ID CUST123456, contacted the bank from
            john.smith@example.com.

            He claims that account 987654321 was charged €20,000 on
            12 August 2026.

            Transaction reference TX998877.
            """.strip();

    @MockitoBean
    private SensitiveDataDetector detector;

    @Autowired
    private PrivacyProcessingService privacyProcessingService;

    @Autowired
    private PolicyRepository policyRepository;

    private PrivacyPolicy bankingPolicy;

    @BeforeEach
    void setUp() {
        bankingPolicy = policyRepository
                .findActivePolicy("demo-tenant")
                .orElseThrow();
    }

    @Test
    void criticalInvariantProtectedValuesAbsentFromSanitizedText() {
        when(detector.detect(anyString())).thenReturn(List.of(
                entity("PERSON", BANKING_TEXT, "John Smith"),
                entity("CUSTOMER_ID", BANKING_TEXT, "CUST123456"),
                entity("EMAIL", BANKING_TEXT, "john.smith@example.com"),
                entity("ACCOUNT_NUMBER", BANKING_TEXT, "987654321"),
                entity("AMOUNT", BANKING_TEXT, "€20,000"),
                entity("DATE", BANKING_TEXT, "12 August 2026"),
                entity("TRANSACTION_ID", BANKING_TEXT, "TX998877")));

        PrivacyProcessingResult result = privacyProcessingService.process(
                "demo-tenant", "conv-banking", BANKING_TEXT, bankingPolicy, "user-1");

        String sanitized = result.sanitizedText();
        assertThat(sanitized)
                .doesNotContain("John Smith")
                .doesNotContain("CUST123456")
                .doesNotContain("john.smith@example.com")
                .doesNotContain("987654321")
                .doesNotContain("12 August 2026")
                .doesNotContain("TX998877")
                .contains("€20,000");

        assertThat(result.entitiesTokenized()).isEqualTo(6);
        assertThat(result.entitiesAllowed()).isEqualTo(1);
        assertThat(sanitized).contains("<<PERSON:").contains("<<EMAIL:");

        PrivacyProcessingResult again = privacyProcessingService.process(
                "demo-tenant", "conv-banking", BANKING_TEXT, bankingPolicy, "user-1");
        assertThat(again.sanitizedText()).isEqualTo(sanitized);
    }

    @Test
    void maskAndRedactUseConfiguredFormats() {
        String text = "Card 4111111111111111 and secret X";
        when(detector.detect(anyString())).thenReturn(List.of(
                new SensitiveEntity("CARD_NUMBER", text.indexOf("4111111111111111"), text.indexOf("4111111111111111") + 16, 0.9),
                new SensitiveEntity("GOVERNMENT_ID", text.indexOf("secret X"), text.indexOf("secret X") + 8, 0.9)));

        // GOVERNMENT_ID unmatched in banking policy → REDACT; CARD_NUMBER → REDACT
        PrivacyProcessingResult result = privacyProcessingService.process(
                "demo-tenant", "conv-mask", text, bankingPolicy, "user-1");

        assertThat(result.sanitizedText()).contains("[REDACTED]");
        assertThat(result.sanitizedText()).doesNotContain("4111111111111111");
        assertThat(result.sanitizedText()).doesNotContain("secret X");
        assertThat(result.entitiesRedacted()).isEqualTo(2);
    }

    @Test
    void denyFailsClosed() {
        PrivacyPolicy denyPolicy = new PrivacyPolicy(
                bankingPolicy.id(),
                bankingPolicy.tenantId(),
                "DENY_TEST",
                "deny person",
                1,
                "ACTIVE",
                List.of(new com.company.privacygateway.privacy.policy.PolicyRule(
                        java.util.UUID.randomUUID(), "PERSON", PolicyAction.DENY, 100)));

        String text = "Hello John Smith";
        when(detector.detect(anyString())).thenReturn(List.of(
                entity("PERSON", text, "John Smith")));

        assertThatThrownBy(() -> privacyProcessingService.process(
                        "demo-tenant", "conv-deny", text, denyPolicy, "user-1"))
                .isInstanceOf(PrivacyProcessingException.class)
                .hasFieldOrPropertyWithValue("code", "PRIVACY_DENIED");
    }

    @Test
    void detectorFailureFailsClosed() {
        when(detector.detect(anyString()))
                .thenThrow(new PrivacyProcessingException("DETECTION_UNAVAILABLE", "down"));

        assertThatThrownBy(() -> privacyProcessingService.process(
                        "demo-tenant", "conv-fail", "hello", bankingPolicy, "user-1"))
                .isInstanceOf(PrivacyProcessingException.class)
                .hasFieldOrPropertyWithValue("code", "DETECTION_UNAVAILABLE");
    }

    @Test
    void maskActionPreservesLengthInCodePoints() {
        String text = "Phone is AB";
        PrivacyPolicy maskPolicy = new PrivacyPolicy(
                bankingPolicy.id(),
                bankingPolicy.tenantId(),
                "MASK_TEST",
                "mask",
                1,
                "ACTIVE",
                List.of(new com.company.privacygateway.privacy.policy.PolicyRule(
                        java.util.UUID.randomUUID(), "PHONE", PolicyAction.MASK, 100)));

        when(detector.detect(anyString())).thenReturn(List.of(
                new SensitiveEntity("PHONE", text.indexOf("AB"), text.indexOf("AB") + 2, 0.9)));

        PrivacyProcessingResult result = privacyProcessingService.process(
                "demo-tenant", "conv-mask2", text, maskPolicy, "user-1");

        assertThat(result.sanitizedText()).isEqualTo("Phone is **");
        assertThat(result.entitiesMasked()).isEqualTo(1);
    }

    private static SensitiveEntity entity(String type, String text, String value) {
        int start = text.indexOf(value);
        if (start < 0) {
            throw new IllegalArgumentException("value not found: " + value);
        }
        int startCp = text.codePointCount(0, start);
        int endCp = startCp + value.codePointCount(0, value.length());
        return new SensitiveEntity(type, startCp, endCp, 0.9);
    }
}
