package com.company.privacygateway.llm.provider;

/**
 * Controlled LLM provider failure. Does not include prompt content in the message.
 */
public class LlmProviderException extends RuntimeException {

    private final String code;

    public LlmProviderException(String code, String message) {
        super(message);
        this.code = code;
    }

    public LlmProviderException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
