# Non-Functional Requirements

## Security

- AES-256-GCM for MVP encrypted vault values.
- Secrets externalized through environment/configuration.
- JWT authentication.
- Tenant isolation.
- Explicit re-identification authorization.
- No plaintext PII in logs/audit.
- No direct LLM-to-vault access.

## Reliability

Privacy processing failure must prevent LLM invocation.

## Maintainability

External providers must be abstracted behind interfaces.

## Testability

Security-sensitive behavior must have automated tests.

## Observability

Provide:
- structured logs
- correlation IDs
- health checks
- privacy metrics
- LLM latency/error metrics

Sensitive values must never be metric labels.

## Configuration

Configure:
- database
- encryption key
- Ollama URL
- Ollama model
- detector URL
- JWT secret
- token TTL

## Local operation

After model download, the MVP must support local inference without requiring a cloud LLM.
