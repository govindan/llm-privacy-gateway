package com.company.privacygateway.privacy.transformation;

import com.company.privacygateway.common.PrivacyProcessingException;
import com.company.privacygateway.privacy.detection.SensitiveDataDetector;
import com.company.privacygateway.privacy.policy.PolicyAction;
import com.company.privacygateway.privacy.policy.PolicyDecision;
import com.company.privacygateway.privacy.policy.PolicyEngine;
import com.company.privacygateway.privacy.policy.PrivacyPolicy;
import com.company.privacygateway.privacy.policy.SensitiveEntity;
import com.company.privacygateway.vault.TokenMapping;
import com.company.privacygateway.vault.TokenVault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PrivacyProcessingService {

    private final SensitiveDataDetector detector;
    private final OverlapResolver overlapResolver;
    private final PolicyEngine policyEngine;
    private final TokenVault tokenVault;

    public PrivacyProcessingService(
            SensitiveDataDetector detector,
            OverlapResolver overlapResolver,
            PolicyEngine policyEngine,
            TokenVault tokenVault) {
        this.detector = detector;
        this.overlapResolver = overlapResolver;
        this.policyEngine = policyEngine;
        this.tokenVault = tokenVault;
    }

    @Transactional
    public PrivacyProcessingResult process(
            String tenantId,
            String conversationId,
            String text,
            PrivacyPolicy policy,
            String createdBy) {
        if (text == null) {
            throw new PrivacyProcessingException("PRIVACY_INVALID_INPUT", "text must not be null");
        }
        if (policy == null) {
            throw new PrivacyProcessingException("PRIVACY_INVALID_INPUT", "policy must not be null");
        }

        List<SensitiveEntity> detected = detector.detect(text);
        List<SensitiveEntity> resolved = overlapResolver.resolve(detected);

        List<PrivacyProcessingResult.AppliedTransformation> transformations = new ArrayList<>();
        for (SensitiveEntity entity : resolved) {
            validateEntityAgainstText(text, entity);
            String originalValue = codePointSlice(text, entity.start(), entity.end());
            PolicyDecision decision = policyEngine.evaluate(entity, policy);
            if (decision.action() == PolicyAction.DENY) {
                throw new PrivacyProcessingException(
                        "PRIVACY_DENIED",
                        "policy denied entity type " + entity.type());
            }
            String replacement = replacementFor(
                    tenantId, conversationId, entity, decision.action(), originalValue, createdBy);
            transformations.add(new PrivacyProcessingResult.AppliedTransformation(
                    entity, decision, originalValue, replacement));
        }

        String sanitized = buildSanitizedText(text, transformations);
        validateNoLeakage(sanitized, transformations);

        return new PrivacyProcessingResult(
                text,
                sanitized,
                transformations,
                detected.size(),
                PrivacyProcessingResult.count(transformations, PolicyAction.TOKENIZE),
                PrivacyProcessingResult.count(transformations, PolicyAction.MASK),
                PrivacyProcessingResult.count(transformations, PolicyAction.REDACT),
                PrivacyProcessingResult.count(transformations, PolicyAction.ALLOW));
    }

    private String replacementFor(
            String tenantId,
            String conversationId,
            SensitiveEntity entity,
            PolicyAction action,
            String originalValue,
            String createdBy) {
        return switch (action) {
            case ALLOW -> originalValue;
            case TOKENIZE -> {
                TokenMapping mapping = tokenVault.createOrGet(
                        tenantId, conversationId, entity.type(), originalValue, createdBy);
                yield mapping.tokenId();
            }
            case MASK -> "*".repeat(originalValue.codePointCount(0, originalValue.length()));
            case REDACT -> "[REDACTED]";
            case DENY -> throw new PrivacyProcessingException(
                    "PRIVACY_DENIED", "policy denied entity type " + entity.type());
        };
    }

    private static String buildSanitizedText(
            String text, List<PrivacyProcessingResult.AppliedTransformation> transformations) {
        if (transformations.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder();
        int cursorCp = 0;
        int totalCp = text.codePointCount(0, text.length());
        for (PrivacyProcessingResult.AppliedTransformation tx : transformations) {
            SensitiveEntity entity = tx.entity();
            if (entity.start() < cursorCp) {
                throw new PrivacyProcessingException(
                        "SANITIZE_OFFSET_ERROR", "overlapping entities survived resolution");
            }
            if (entity.start() > cursorCp) {
                out.append(codePointSlice(text, cursorCp, entity.start()));
            }
            out.append(tx.replacement());
            cursorCp = entity.end();
        }
        if (cursorCp < totalCp) {
            out.append(codePointSlice(text, cursorCp, totalCp));
        }
        return out.toString();
    }

    private static void validateNoLeakage(
            String sanitized, List<PrivacyProcessingResult.AppliedTransformation> transformations) {
        for (PrivacyProcessingResult.AppliedTransformation tx : transformations) {
            PolicyAction action = tx.decision().action();
            if (action == PolicyAction.ALLOW) {
                continue;
            }
            if (sanitized.contains(tx.originalValue())) {
                throw new PrivacyProcessingException(
                        "SANITIZE_VALIDATION_FAILED",
                        "protected value remained after " + action + " for type " + tx.entity().type());
            }
        }
    }

    private static void validateEntityAgainstText(String text, SensitiveEntity entity) {
        int totalCp = text.codePointCount(0, text.length());
        if (entity.end() > totalCp) {
            throw new PrivacyProcessingException(
                    "DETECTION_INVALID_OFFSETS",
                    "entity offsets exceed text length for type " + entity.type());
        }
    }

    public static String codePointSlice(String text, int startCp, int endCp) {
        int startIndex = text.offsetByCodePoints(0, startCp);
        int endIndex = text.offsetByCodePoints(0, endCp);
        return text.substring(startIndex, endIndex);
    }
}
