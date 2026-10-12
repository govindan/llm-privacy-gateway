# Product Vision

## Product

**LLM Privacy Gateway**

## Vision

Provide a privacy control plane between enterprise applications/data and generative AI systems.

The gateway allows useful LLM reasoning over protected data without requiring the LLM to see the original sensitive values.

## MVP proposition

Demonstrate:

1. Detect sensitive data.
2. Apply a configurable policy.
3. Tokenize/mask/redact protected data.
4. Send only sanitized content to local Ollama.
5. Inspect the LLM response.
6. Re-identify only when the caller is authorized.
7. Produce an auditable record without logging plaintext sensitive data.

## Canonical scenario

A banking customer complaint contains customer identity, email, account number, transaction reference, date and monetary amount.

The LLM should be able to summarize the complaint while receiving tokens for protected entities.

## Product boundary

The MVP is a local architectural proof of concept. It is not a production-ready enterprise security platform.

Future capabilities may include RAG, agents, multiple LLM providers, enterprise IAM, KMS/HSM, SIEM, data residency and policy-as-code.
