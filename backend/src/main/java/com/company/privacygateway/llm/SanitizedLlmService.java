package com.company.privacygateway.llm;

import com.company.privacygateway.llm.provider.LlmProvider;
import com.company.privacygateway.llm.provider.LlmRequest;
import com.company.privacygateway.llm.provider.LlmResponse;
import com.company.privacygateway.privacy.policy.PolicyRepository;
import com.company.privacygateway.privacy.policy.PrivacyPolicy;
import com.company.privacygateway.privacy.response.ResponsePrivacyResult;
import com.company.privacygateway.privacy.response.ResponsePrivacyService;
import com.company.privacygateway.privacy.transformation.PrivacyProcessingResult;
import com.company.privacygateway.privacy.transformation.PrivacyProcessingService;
import org.springframework.stereotype.Service;

/**
 * Sanitizes prompts before the LLM and inspects responses afterward.
 * Vault resolve happens only via ResponsePrivacyService when re-id is authorized.
 */
@Service
public class SanitizedLlmService {

    private final PrivacyProcessingService privacyProcessingService;
    private final PolicyRepository policyRepository;
    private final LlmProvider llmProvider;
    private final ResponsePrivacyService responsePrivacyService;

    public SanitizedLlmService(
            PrivacyProcessingService privacyProcessingService,
            PolicyRepository policyRepository,
            LlmProvider llmProvider,
            ResponsePrivacyService responsePrivacyService) {
        this.privacyProcessingService = privacyProcessingService;
        this.policyRepository = policyRepository;
        this.llmProvider = llmProvider;
        this.responsePrivacyService = responsePrivacyService;
    }

    public SanitizedLlmResult chat(
            String tenantId,
            String conversationId,
            String originalMessage,
            String createdBy,
            boolean reidentifyAuthorized) {
        PrivacyPolicy policy = policyRepository
                .findActivePolicy(tenantId)
                .orElseThrow(() -> new IllegalStateException("no active privacy policy for tenant"));

        PrivacyProcessingResult privacy = privacyProcessingService.process(
                tenantId, conversationId, originalMessage, policy, createdBy);

        LlmResponse llmResponse = llmProvider.chat(new LlmRequest(privacy.sanitizedText()));

        ResponsePrivacyResult responsePrivacy = responsePrivacyService.process(
                tenantId,
                conversationId,
                llmResponse.content(),
                reidentifyAuthorized);

        return new SanitizedLlmResult(privacy, llmResponse, responsePrivacy);
    }

    public record SanitizedLlmResult(
            PrivacyProcessingResult privacy,
            LlmResponse llmResponse,
            ResponsePrivacyResult responsePrivacy) {
    }
}
