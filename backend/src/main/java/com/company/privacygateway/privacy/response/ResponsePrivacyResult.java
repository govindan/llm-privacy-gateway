package com.company.privacygateway.privacy.response;

import java.util.List;

/**
 * User-facing LLM response after inspection and optional re-identification.
 */
public record ResponsePrivacyResult(
        String rawLlmText,
        String userFacingText,
        boolean reidentifyAuthorized,
        int knownTokensFound,
        int knownTokensResolved,
        int newEntitiesRedacted
) {
    public ResponsePrivacyResult {
        if (rawLlmText == null || userFacingText == null) {
            throw new IllegalArgumentException("response texts must not be null");
        }
    }
}
