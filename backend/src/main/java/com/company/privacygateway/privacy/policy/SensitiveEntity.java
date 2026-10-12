package com.company.privacygateway.privacy.policy;

/**
 * Minimal entity view for policy evaluation. Confidence is ignored in MVP (D-008).
 */
public record SensitiveEntity(String type, int start, int end, Double score) {

    public SensitiveEntity {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("entity type must not be blank");
        }
    }

    public SensitiveEntity(String type) {
        this(type, 0, 0, null);
    }
}
