package com.company.privacygateway.llm.provider;

public interface LlmProvider {

    LlmResponse chat(LlmRequest request);
}
