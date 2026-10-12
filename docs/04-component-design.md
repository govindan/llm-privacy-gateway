# Component Design

## Backend packages

```text
com.company.privacygateway
├── api
├── auth
├── conversation
├── privacy
│   ├── detection
│   ├── policy
│   ├── transformation
│   └── response
├── vault
├── llm
│   ├── provider
│   └── ollama
├── audit
├── security
├── configuration
└── common
```

## Core interfaces

```java
interface LlmProvider {
    LlmResponse chat(LlmRequest request);
}

interface EncryptionService {
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}

interface SensitiveDataDetector {
    List<SensitiveEntity> detect(String text);
}

interface PolicyEngine {
    PolicyDecision evaluate(SensitiveEntity entity, PrivacyPolicy policy);
}

interface TokenVault {
    TokenMapping createOrGet(...);
    Optional<TokenMapping> resolve(...);
}
```

Exact signatures may be refined during implementation but material changes must be reported.

## Processing service

Conceptual operation:

```java
PrivacyProcessingResult process(
    String tenantId,
    String conversationId,
    String text,
    PrivacyPolicy policy
);
```

## Replacement algorithm

Do not perform sequential global string replacements.

1. Detect all entities.
2. Resolve overlaps deterministically.
3. Sort accepted entities by source offset.
4. Construct sanitized output from original offsets.
5. Validate that protected values do not remain when policy requires transformation.
