# Ollama Integration

## Principle

Ollama is a provider implementation behind an abstraction.

```java
interface LlmProvider {
    LlmResponse chat(LlmRequest request);
}
```

Implementation:

```text
OllamaLlmProvider
```

Configuration:

```yaml
llm:
  provider: ollama
  base-url: ${OLLAMA_BASE_URL}
  model: ${OLLAMA_MODEL}
```

Do not hard-code the model name.

## Rules

- Only sanitized requests may reach Ollama.
- Ollama must not receive vault credentials.
- Ollama must not access PostgreSQL directly.
- Ollama must not access encryption keys.
- Provider failures must return controlled errors.
- Timeouts must be configured.

## Future providers

The architecture should permit:
- OpenAI
- Azure OpenAI
- Anthropic
- Gemini
- Bedrock
- other local inference providers

without changing privacy-domain logic.
