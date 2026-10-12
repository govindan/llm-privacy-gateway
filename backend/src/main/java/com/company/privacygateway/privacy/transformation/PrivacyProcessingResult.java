package com.company.privacygateway.privacy.transformation;

import com.company.privacygateway.privacy.policy.PolicyAction;
import com.company.privacygateway.privacy.policy.PolicyDecision;
import com.company.privacygateway.privacy.policy.SensitiveEntity;

import java.util.List;

public record PrivacyProcessingResult(
        String originalText,
        String sanitizedText,
        List<AppliedTransformation> transformations,
        int entitiesDetected,
        int entitiesTokenized,
        int entitiesMasked,
        int entitiesRedacted,
        int entitiesAllowed
) {
    public PrivacyProcessingResult {
        transformations = List.copyOf(transformations);
    }

    public record AppliedTransformation(
            SensitiveEntity entity,
            PolicyDecision decision,
            String originalValue,
            String replacement
    ) {
    }

    public static int count(List<AppliedTransformation> transformations, PolicyAction action) {
        return (int) transformations.stream().filter(t -> t.decision().action() == action).count();
    }
}
