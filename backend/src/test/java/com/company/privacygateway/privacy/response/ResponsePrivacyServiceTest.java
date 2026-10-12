package com.company.privacygateway.privacy.response;

import com.company.privacygateway.common.PrivacyProcessingException;
import com.company.privacygateway.privacy.detection.SensitiveDataDetector;
import com.company.privacygateway.privacy.policy.PolicyRepository;
import com.company.privacygateway.privacy.policy.PrivacyPolicy;
import com.company.privacygateway.privacy.policy.SensitiveEntity;
import com.company.privacygateway.privacy.transformation.PrivacyProcessingService;
import com.company.privacygateway.vault.TokenMapping;
import com.company.privacygateway.vault.TokenVault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
class ResponsePrivacyServiceTest {

    @MockitoBean
    private SensitiveDataDetector detector;

    @Autowired
    private ResponsePrivacyService responsePrivacyService;

    @Autowired
    private TokenVault tokenVault;

    @Autowired
    private PrivacyProcessingService privacyProcessingService;

    @Autowired
    private PolicyRepository policyRepository;

    private PrivacyPolicy bankingPolicy;
    private String personToken;
    private String emailToken;

    @BeforeEach
    void setUp() {
        bankingPolicy = policyRepository.findActivePolicy("demo-tenant").orElseThrow();

        String source = "Contact John Smith at john.smith@example.com please.";
        when(detector.detect(eq(source))).thenReturn(List.of(
                entity("PERSON", source, "John Smith"),
                entity("EMAIL", source, "john.smith@example.com")));

        var privacy = privacyProcessingService.process(
                "demo-tenant", "conv-response-privacy", source, bankingPolicy, "user-1");
        personToken = privacy.transformations().stream()
                .filter(t -> t.entity().type().equals("PERSON"))
                .findFirst()
                .orElseThrow()
                .replacement();
        emailToken = privacy.transformations().stream()
                .filter(t -> t.entity().type().equals("EMAIL"))
                .findFirst()
                .orElseThrow()
                .replacement();
    }

    @Test
    void unauthorizedKeepsTokensAndDoesNotResolve() {
        String llmText = "Customer " + personToken + " emailed " + emailToken + ".";
        when(detector.detect(eq(llmText))).thenReturn(List.of());

        ResponsePrivacyResult result = responsePrivacyService.process(
                "demo-tenant", "conv-response-privacy", llmText, false);

        assertThat(result.reidentifyAuthorized()).isFalse();
        assertThat(result.userFacingText()).contains(personToken).contains(emailToken);
        assertThat(result.userFacingText()).doesNotContain("John Smith").doesNotContain("john.smith@example.com");
        assertThat(result.knownTokensFound()).isEqualTo(2);
        assertThat(result.knownTokensResolved()).isZero();
    }

    @Test
    void authorizedReidentifiesKnownTokens() {
        String llmText = "Customer " + personToken + " emailed " + emailToken + ".";
        when(detector.detect(eq(llmText))).thenReturn(List.of());

        ResponsePrivacyResult result = responsePrivacyService.process(
                "demo-tenant", "conv-response-privacy", llmText, true);

        assertThat(result.reidentifyAuthorized()).isTrue();
        assertThat(result.userFacingText())
                .contains("John Smith")
                .contains("john.smith@example.com")
                .doesNotContain(personToken)
                .doesNotContain(emailToken);
        assertThat(result.knownTokensResolved()).isEqualTo(2);
    }

    @Test
    void newSensitiveSpansInResponseAreRedactedNotVaulted() {
        String hallucinated = "Also call Jane Doe at home.";
        String llmText = "Customer " + personToken + ". " + hallucinated;
        when(detector.detect(eq(llmText))).thenReturn(List.of(
                entity("PERSON", llmText, "Jane Doe")));

        int vaultSizeBefore = countMappings("demo-tenant", "conv-response-privacy");

        ResponsePrivacyResult unauthorized = responsePrivacyService.process(
                "demo-tenant", "conv-response-privacy", llmText, false);

        assertThat(unauthorized.userFacingText())
                .contains(personToken)
                .contains("[REDACTED]")
                .doesNotContain("Jane Doe");
        assertThat(unauthorized.newEntitiesRedacted()).isEqualTo(1);

        ResponsePrivacyResult authorized = responsePrivacyService.process(
                "demo-tenant", "conv-response-privacy", llmText, true);

        assertThat(authorized.userFacingText())
                .contains("John Smith")
                .contains("[REDACTED]")
                .doesNotContain("Jane Doe");

        assertThat(countMappings("demo-tenant", "conv-response-privacy")).isEqualTo(vaultSizeBefore);
    }

    @Test
    void detectorFailureOnResponseFailsClosed() {
        String llmText = "Customer " + personToken + ".";
        when(detector.detect(eq(llmText)))
                .thenThrow(new PrivacyProcessingException("DETECTION_UNAVAILABLE", "down"));

        assertThatThrownBy(() -> responsePrivacyService.process(
                        "demo-tenant", "conv-response-privacy", llmText, true))
                .isInstanceOf(PrivacyProcessingException.class)
                .hasFieldOrPropertyWithValue("code", "DETECTION_UNAVAILABLE");
    }

    private int countMappings(String tenantId, String conversationId) {
        // Resolve known tokens to prove vault still only has original two mappings.
        assertThat(tokenVault.resolve(tenantId, conversationId, personToken)).isPresent();
        assertThat(tokenVault.resolve(tenantId, conversationId, emailToken)).isPresent();
        TokenMapping person = tokenVault.resolve(tenantId, conversationId, personToken).orElseThrow();
        assertThat(tokenVault.decryptValue(person)).isEqualTo("John Smith");
        return 2;
    }

    private static SensitiveEntity entity(String type, String text, String value) {
        int start = text.indexOf(value);
        int startCp = text.codePointCount(0, start);
        int endCp = startCp + value.codePointCount(0, value.length());
        return new SensitiveEntity(type, startCp, endCp, 0.9);
    }
}
