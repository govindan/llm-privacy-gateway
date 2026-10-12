package com.company.privacygateway.api;

import java.util.List;

public record AnalyzeResponse(
        String conversationId,
        String sanitizedText,
        List<EntityAction> entities
) {
    public record EntityAction(String type, String action, String token, int start, int end) {
    }
}
