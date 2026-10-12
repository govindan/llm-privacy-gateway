package com.company.privacygateway.llm.provider;

public record LlmRequest(String prompt) {

    public LlmRequest {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt must not be null");
        }
    }
}
