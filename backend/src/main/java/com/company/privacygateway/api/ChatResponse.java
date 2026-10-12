package com.company.privacygateway.api;

public record ChatResponse(
        String conversationId,
        String response,
        PrivacySummary privacy
) {
    public record PrivacySummary(int entitiesDetected, int entitiesTokenized, int entitiesRedacted) {
    }
}
