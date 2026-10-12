package com.company.privacygateway.api;

import com.company.privacygateway.auth.GatewayPrincipal;
import com.company.privacygateway.audit.AuditService;
import com.company.privacygateway.common.CorrelationIdFilter;
import com.company.privacygateway.conversation.ConversationRepository;
import com.company.privacygateway.llm.SanitizedLlmService;
import com.company.privacygateway.privacy.policy.PolicyAction;
import com.company.privacygateway.privacy.policy.PolicyRepository;
import com.company.privacygateway.privacy.policy.PrivacyPolicy;
import com.company.privacygateway.privacy.transformation.PrivacyProcessingResult;
import com.company.privacygateway.privacy.transformation.PrivacyProcessingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class PrivacyGatewayController {

    private final ConversationRepository conversationRepository;
    private final SanitizedLlmService sanitizedLlmService;
    private final PrivacyProcessingService privacyProcessingService;
    private final PolicyRepository policyRepository;
    private final AuditService auditService;

    public PrivacyGatewayController(
            ConversationRepository conversationRepository,
            SanitizedLlmService sanitizedLlmService,
            PrivacyProcessingService privacyProcessingService,
            PolicyRepository policyRepository,
            AuditService auditService) {
        this.conversationRepository = conversationRepository;
        this.sanitizedLlmService = sanitizedLlmService;
        this.privacyProcessingService = privacyProcessingService;
        this.policyRepository = policyRepository;
        this.auditService = auditService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        GatewayPrincipal principal = principal(authentication);
        String correlationId = correlationId(httpRequest);
        conversationRepository.ensureExists(
                principal.tenantId(), request.conversationId(), principal.userId());

        PrivacyPolicy policy = policyRepository
                .findActivePolicy(principal.tenantId())
                .orElseThrow(() -> new IllegalStateException("no active privacy policy for tenant"));

        try {
            SanitizedLlmService.SanitizedLlmResult result = sanitizedLlmService.chat(
                    principal.tenantId(),
                    request.conversationId(),
                    request.message(),
                    principal.userId(),
                    principal.reidentify());

            auditService.record(
                    principal.tenantId(),
                    request.conversationId(),
                    principal.userId(),
                    "CHAT",
                    policy.id(),
                    null,
                    null,
                    true,
                    "detected=" + result.privacy().entitiesDetected()
                            + ";tokenized=" + result.privacy().entitiesTokenized()
                            + ";redacted=" + result.privacy().entitiesRedacted()
                            + ";reidentify=" + principal.reidentify(),
                    correlationId);

            return new ChatResponse(
                    request.conversationId(),
                    result.responsePrivacy().userFacingText(),
                    new ChatResponse.PrivacySummary(
                            result.privacy().entitiesDetected(),
                            result.privacy().entitiesTokenized(),
                            result.privacy().entitiesRedacted()));
        } catch (RuntimeException ex) {
            auditService.record(
                    principal.tenantId(),
                    request.conversationId(),
                    principal.userId(),
                    "CHAT",
                    policy.id(),
                    null,
                    null,
                    false,
                    "failure",
                    correlationId);
            throw ex;
        }
    }

    @PostMapping("/privacy/analyze")
    public AnalyzeResponse analyze(
            @Valid @RequestBody AnalyzeRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        GatewayPrincipal principal = principal(authentication);
        String correlationId = correlationId(httpRequest);
        conversationRepository.ensureExists(
                principal.tenantId(), request.conversationId(), principal.userId());

        PrivacyPolicy policy = policyRepository
                .findActivePolicy(principal.tenantId())
                .orElseThrow(() -> new IllegalStateException("no active privacy policy for tenant"));

        PrivacyProcessingResult privacy = privacyProcessingService.process(
                principal.tenantId(),
                request.conversationId(),
                request.text(),
                policy,
                principal.userId());

        for (PrivacyProcessingResult.AppliedTransformation tx : privacy.transformations()) {
            auditService.record(
                    principal.tenantId(),
                    request.conversationId(),
                    principal.userId(),
                    "ANALYZE",
                    policy.id(),
                    tx.entity().type(),
                    tx.decision().action().name(),
                    true,
                    "action=" + tx.decision().action().name(),
                    correlationId);
        }

        return new AnalyzeResponse(
                request.conversationId(),
                privacy.sanitizedText(),
                privacy.transformations().stream()
                        .map(tx -> new AnalyzeResponse.EntityAction(
                                tx.entity().type(),
                                tx.decision().action().name(),
                                tx.decision().action() == PolicyAction.TOKENIZE ? tx.replacement() : null,
                                tx.entity().start(),
                                tx.entity().end()))
                        .toList());
    }

    @GetMapping("/audit/conversations/{conversationId}")
    public AuditConversationResponse audit(
            @PathVariable String conversationId, Authentication authentication) {
        GatewayPrincipal principal = principal(authentication);
        var events = auditService.listForConversation(principal.tenantId(), conversationId).stream()
                .map(event -> new AuditConversationResponse.AuditItem(
                        event.id(),
                        event.timestamp(),
                        event.userId(),
                        event.operation(),
                        event.policyId(),
                        event.entityType(),
                        event.action(),
                        event.success(),
                        event.metadata(),
                        event.correlationId()))
                .toList();
        return new AuditConversationResponse(conversationId, events);
    }

    private static GatewayPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof GatewayPrincipal principal)) {
            throw new IllegalArgumentException("authenticated principal required");
        }
        return principal;
    }

    private static String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return value == null ? null : value.toString();
    }
}
