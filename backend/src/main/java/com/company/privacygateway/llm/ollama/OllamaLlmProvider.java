package com.company.privacygateway.llm.ollama;

import com.company.privacygateway.configuration.LlmProperties;
import com.company.privacygateway.llm.provider.LlmProvider;
import com.company.privacygateway.llm.provider.LlmProviderException;
import com.company.privacygateway.llm.provider.LlmRequest;
import com.company.privacygateway.llm.provider.LlmResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Ollama adapter only. Must receive already-sanitized prompts.
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaLlmProvider implements LlmProvider {

    private final RestClient restClient;
    private final LlmProperties properties;

    public OllamaLlmProvider(LlmProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        String model = properties.getModel();
        if (model == null || model.isBlank()) {
            throw new LlmProviderException("LLM_MODEL_MISSING", "OLLAMA_MODEL is not configured");
        }
        try {
            OllamaChatResponse response = restClient
                    .post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "stream", false,
                            "messages", List.of(Map.of(
                                    "role", "user",
                                    "content", request.prompt()))))
                    .retrieve()
                    .body(OllamaChatResponse.class);
            if (response == null || response.message() == null || response.message().content() == null) {
                throw new LlmProviderException("LLM_INVALID_RESPONSE", "Ollama returned an empty response");
            }
            return new LlmResponse(response.message().content(), response.model() == null ? model : response.model());
        } catch (LlmProviderException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new LlmProviderException("LLM_UNAVAILABLE", "Ollama request failed", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaChatResponse(String model, OllamaMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaMessage(String role, String content) {
    }
}
