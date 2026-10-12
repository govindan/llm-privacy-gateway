# Cursor Start Prompt

You are the lead software architect and implementation engineer for this repository.

We are building an end-to-end proof of concept for an enterprise-style **LLM Privacy Gateway**.

The product objective is:

> Allow an application to use sensitive enterprise data with an LLM while ensuring protected sensitive values are transformed before reaching the LLM.

The initial LLM runtime is **local Ollama**.

## First instruction — DO NOT CODE YET

Before writing application code, inspect this repository and read:

- `README.md`
- all files under `docs/`
- all files under `.cursor/rules/`

Treat those documents as the governing specification.

Then produce an **Implementation Readiness Assessment** containing:

1. Your understanding of the product
2. System/component architecture
3. Trust boundaries
4. End-to-end data flow
5. Major domain objects
6. Major interfaces
7. API surface
8. Database responsibilities
9. Ollama integration approach
10. Privacy processing lifecycle
11. Security controls
12. Implementation phases
13. Test strategy
14. Ambiguities, contradictions or missing decisions

Do not invent architecture where the specification is silent. Identify the decision that is missing.

## Non-negotiable privacy flow

The intended flow is:

Original request
→ authentication
→ sensitive-data detection
→ policy evaluation
→ tokenization/masking/redaction
→ sanitized request
→ Ollama
→ response inspection
→ authorized token resolution/re-identification
→ user

The LLM must never have direct access to:

- the privacy vault
- plaintext sensitive values
- encryption keys
- the database
- token-resolution APIs
- enterprise credentials

Privacy processing must fail closed. If the gateway cannot establish that a request is safe to send, the original request must not be sent to the LLM.

## Engineering principles

- Keep privacy logic independent from Ollama.
- Use an LLM provider interface.
- Keep controllers thin.
- Keep domain logic testable.
- Never log plaintext sensitive values.
- Never hard-code secrets.
- Use configuration for environment-specific values.
- Maintain tenant isolation in the data model.
- Prefer explicit interfaces over hidden coupling.
- Write tests alongside implementation.
- Do not introduce unnecessary technologies.
- Do not modify unrelated files.
- Do not claim a security control exists unless code/tests actually enforce it.

## Implementation process

After the readiness assessment, WAIT.

Do not implement Phase 1 until explicitly instructed.

When implementation is approved:

1. State the phase being implemented.
2. Identify the governing requirements and acceptance criteria.
3. Implement only that phase.
4. Run compilation/lint/tests applicable to the phase.
5. Report failures and fixes.
6. Re-check the acceptance criteria.
7. Summarize files changed.
8. Stop and wait for the next phase.

The first implementation phase will be defined in `docs/14-implementation-plan.md`.
