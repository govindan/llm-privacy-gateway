package com.company.privacygateway.privacy.policy;

import java.util.Locale;

public enum PolicyAction {
    ALLOW,
    TOKENIZE,
    MASK,
    REDACT,
    DENY;

    public static PolicyAction fromStorage(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("policy action must not be blank");
        }
        return PolicyAction.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
