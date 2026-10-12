# Acceptance Criteria

The MVP is accepted only when the following end-to-end test works.

## Input

```text
John Smith from john.smith@example.com transferred
€20,000 from account 123456789.
Transaction reference TX998877.
```

## Required behavior

1. User authenticates.
2. Gateway receives original request.
3. Detector identifies protected entities.
4. Policy determines actions.
5. Protected values are replaced with opaque tokens.
6. Token mappings are encrypted.
7. Sanitized prompt is sent to Ollama.
8. Original protected values are absent from the Ollama request.
9. Ollama generates a response.
10. Response is inspected.
11. Authorized token resolution works.
12. Unauthorized resolution does not work.
13. Audit events are generated.
14. No plaintext protected value appears in application logs.
15. Detector failure prevents Ollama invocation.

## Architectural proof

The demonstration must make it possible to answer:

> Can the LLM perform useful reasoning over a request containing protected enterprise data without receiving the underlying protected values?

A successful MVP must provide evidence for both:
- functional usefulness
- privacy boundary enforcement
