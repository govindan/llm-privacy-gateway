package com.company.privacygateway.common;

/**
 * Fail-closed privacy processing failure. Callers must not invoke the LLM.
 */
public class PrivacyProcessingException extends RuntimeException {

    private final String code;

    public PrivacyProcessingException(String code, String message) {
        super(message);
        this.code = code;
    }

    public PrivacyProcessingException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
