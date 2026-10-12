# System Architecture

## Logical architecture

```text
User/Application
      |
      v
+-----------------------+
| LLM Privacy Gateway   |
|-----------------------|
| Auth                  |
| Request orchestration |
| Policy enforcement    |
| Token orchestration   |
| Response inspection   |
+----+----------+-------+
     |          |
     v          v
PII Detector  Token Vault
     |          |
     +----+-----+
          |
          v
   LLM Provider
          |
          v
       Ollama
```

## Components

### React UI

Demonstration and operator-facing visualization.

### Spring Boot Gateway

Primary API, authentication, orchestration, policy and provider abstraction.

### Python Privacy Detector

Independent service for sensitive-data recognition.

### PostgreSQL

Persistence for encrypted token mappings, policies, conversations and audit metadata.

### Token Vault

Logical domain within the backend responsible for protected value storage and controlled resolution.

### Policy Engine

Determines what action applies to each detected entity.

### Ollama Provider

Adapter to local Ollama inference.

## Trust boundaries

1. User/application to gateway.
2. Gateway to detector.
3. Gateway to vault/database.
4. Gateway to LLM.
5. Authorized re-identification boundary.

The LLM boundary is critical: protected values must not cross it.

## Privacy pipeline

```text
Original
 -> Detect
 -> Classify
 -> Policy
 -> Transform
 -> Validate
 -> LLM
 -> Inspect response
 -> Resolve authorized tokens
 -> User
```

## Fail-closed invariant

If detection, policy evaluation, transformation or validation fails, the request must not be sent to the LLM.
