# Decision Record — Phase 0 blockers

**Status:** Accepted.

**Accepted:** 2026-09-14, D-001–D-018 as written (no amendments).

**Purpose:** Close only the decisions that would otherwise force silent invention during implementation. This record does not replace `docs/01`–`docs/15`. Where this record and an earlier doc conflict, **this record governs**.

**Rule:** Phase 1 may proceed under `docs/14-implementation-plan.md` when explicitly instructed. Temporary assumptions still expire at the phase listed on each decision.

**Implementation note (2026-10-12):** Phases 1–9 are implemented in this repository. Phase 10 (e2e hardening per `docs/14-implementation-plan.md`) is not complete. Historical “Expires: Phase N” lines below describe when each assumption had to be resolved during build-out; they are not a claim that later phases are unfinished unless stated above.

Temporary assumptions include an **expiry phase**. After that phase, the assumption must be replaced by a real control or an amended requirement.

---



## How to read this


| Field                 | Meaning                                                        |
| --------------------- | -------------------------------------------------------------- |
| Decision              | The rule to implement                                          |
| Source                | Specified / Interpretation / Assumption                        |
| Expires               | Phase after which the assumption is no longer acceptable as-is |
| Rejected alternatives | What we are not doing, so later phases do not reopen quietly   |


**Specified** = already in governing docs; restated so implementers do not re-litigate.  
**Interpretation** = resolves a conflict between governing docs.  
**Assumption** = not in the spec; needed to proceed; must stay fail-closed.

---



## D-001 — Document precedence

**Source:** Specified (README, `00-governing-principles`) plus this record.

**Decision:** `/docs` governs. If two requirements conflict, do not pick silently. After ratification, this file is the conflict-resolution log. Material architecture not in `/docs` or this file must not be invented.

---



## D-002 — Residual disclosures (accepted for MVP)

**Source:** Interpretation of token format, FR-005, sample policy, architecture.

**Decision:** The MVP **accepts** these leaks and must not pretend they are closed:

1. Typed tokens (`<<EMAIL:…>>`) disclose entity class to the LLM and to clients who see sanitized text.
2. Deterministic tokens within `tenant + conversation + entity/value` disclose equality of repeated values.
3. Sample policy `AMOUNT: ALLOW` discloses monetary magnitude.
4. The detector receives plaintext (trust boundary 2).
5. The demonstration UI may display original plaintext **in the browser only**.

These are residual risks, not defects to “fix” in later phases unless a new decision says so.

---



## D-003 — Original prompt persistence

**Source:** Assumption (threat model vs FR-012).

**Expires:** Phase 9 (must be re-confirmed when the UI is built).

**Decision:** The backend **must not** persist original (plaintext) prompts or protected values except as AES-256-GCM vault mappings required for tokenization.

The four-pane UI shows “original” from **client-held state** for that session (the request the user just submitted), not from a server stored copy.

**Rejected:** Encrypted archive of raw prompts for operator replay; server-side “original message” table.

**Phase 9 reconfirmation (2026-09-15):** Reconfirmed. The React demo renders pane 1 exclusively from browser session state after submit; no server “original prompt” API or table was added.

---



## D-004 — Phase 1 technology defaults

**Source:** Assumption (stack is specified; versions are not).

**Expires:** Phase 1 (versions may be patched; language/runtime family must not change without a new decision).

**Decision:**


| Layer         | Default                                                            |
| ------------- | ------------------------------------------------------------------ |
| Backend       | Java 21, Spring Boot 3.4.x                                         |
| Java package  | `com.company.privacygateway` (as in `docs/04-component-design.md`) |
| Detector      | Python 3.12                                                        |
| Frontend      | React 18, Vite                                                     |
| Database      | PostgreSQL 16                                                      |
| Orchestration | Docker Compose as in `docs/13-docker-deployment.md`                |


**Rejected:** Cloud LLM as the MVP provider; merging the detector into the JVM process (the detector remains an independent service).

---



## D-005 — Health endpoints

**Source:** Assumption (NFR requires health; paths unspecified).

**Expires:** Phase 1.

**Decision:**


| Service           | Path                                                  | Auth   |
| ----------------- | ----------------------------------------------------- | ------ |
| Gateway           | `GET /actuator/health`                                | Public |
| Detector          | `GET /health`                                         | Public |
| Frontend          | HTTP 200 on `/` or a dedicated `/health`              | Public |
| Postgres / Ollama | Compose healthchecks only; not part of the public API |        |


All `/api/v1/*` routes require authentication. Health is **not** under `/api/v1` except if a later decision adds a duplicate authenticated admin health.

---



## D-006 — Authentication timing

**Source:** Interpretation (FR-001 and API spec vs Phase 8).

**Expires:** Phase 8 (replace test principals / seed JWTs with the full auth+audit design).

**Decision:** From the first phase that exposes `/api/v1`, **every** such endpoint requires a JWT. Phase 8 implements audit completeness and the production-shaped claim set; it does **not** introduce authentication for the first time.

Until Phase 8:

- HS256 JWT, secret from `JWT_SECRET`.
- Seed or document a **dev/test** token mint (never a committed production secret).
- Required claims: `sub` (user id), `tenant_id`, `reidentify` (boolean).
- Missing/invalid JWT → 401. No anonymous chat, analyze, vault, or audit.

**Rejected:** Open APIs until Phase 8; a hard-coded header such as `X-User-Id` as the only control.

---



## D-007 — Tenant and conversation binding

**Source:** Interpretation of data model + API examples.

**Expires:** Phase 2 for tenant enforcement in persistence; Phase 5 for conversation-scoped tokens.

**Decision:**

- `tenant_id` comes **only** from the JWT, never from an untrusted request field.
- `POST /chat` and `POST /privacy/analyze` require a `conversationId`. If the conversation does not exist for that tenant, the gateway **creates** it (same id, that tenant). No public conversation-create API in MVP.
- All vault and audit operations are tenant-scoped; cross-tenant id reuse must not resolve.

**Rejected:** Tenant in JSON body; global (non-tenant) token mappings.

---



## D-008 — Policy selection and unmatched types

**Source:** Interpretation (sample policy vs incomplete request schema).

**Expires:** Phase 4.

**Decision:**

- MVP uses a single active policy per tenant. Seed `BANKING_STANDARD` for the demo tenant. No `policyId` on chat requests in MVP.
- If a detector entity type has **no** matching `policy_rule`, the action is **REDACT**. Never ALLOW by omission.
- `REVIEW` is **out of MVP**. Do not persist or return REVIEW. If a detector supplies confidence, ignore it in MVP (no threshold routing).

**Rejected:** Request-level policy override; implicit ALLOW for unlisted types; implementing REVIEW as a first-class action.

---



## D-009 — Entity type set (DATE vs DATE_OF_BIRTH)

**Source:** Interpretation (`docs/01` vs `docs/12` banking scenario).

**Expires:** Phase 3.

**Decision:** Detector and policy **must** support all FR-002 types **plus** `DATE`.

- `DATE_OF_BIRTH` — recognizers aimed at DoB (explicit labels, `dob`, `born`, structured DoB).
- `DATE` — calendar dates that are not classified as DoB, including the banking scenario date `12 August 2026`.

Seed banking policy: `DATE` → **TOKENIZE**, `DATE_OF_BIRTH` → **TOKENIZE** (same as spec sample for DoB).

E2E banking tests must expect `DATE` (and other listed types) as in `docs/12-test-strategy.md`.

**Rejected:** Shipping FR-002 only and treating the e2e `DATE` expectation as optional; collapsing all dates into `DATE_OF_BIRTH`.

---



## D-010 — Overlap resolution

**Source:** Assumption (`docs/04` requires a deterministic rule but does not define it).

**Expires:** Phase 5.

**Decision:** After detection, before policy/transform:

1. Drop fully contained spans (keep the longer span).
2. For partial overlap, keep the longer span; if equal length, keep the **higher detector confidence** if present, else the **leftmost** span.
3. Sort surviving spans by start offset ascending, then end offset descending.
4. Build sanitized text from those offsets only. No sequential global replace.

If overlap resolution cannot complete, **fail closed** (do not call the LLM).

---



## D-011 — Transformation actions

**Source:** Assumption (FR-003 names actions; formats unspecified).

**Expires:** Phase 5.

**Decision:**


| Action   | Behavior                                                                                                                                                                                                     |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| ALLOW    | Copy original span unchanged.                                                                                                                                                                                |
| TOKENIZE | Replace with opaque token `<<{TYPE}:{ID}>>`. `{ID}` is cryptographically random, hex, **16 characters**. No plaintext in the token. Reuse mapping for same tenant + conversation + entity type + value hash. |
| MASK     | Replace each character in the span with `*`. Length preserved.                                                                                                                                               |
| REDACT   | Replace the entire span with `[REDACTED]`.                                                                                                                                                                   |
| DENY     | **Fail the request.** Do not call the LLM. Structured error; HTTP **403**. No partial send.                                                                                                                  |


Validation after transform: for every span whose action was TOKENIZE, MASK, or REDACT, the original substring **must not** appear in the sanitized prompt. If it does, fail closed.

**Rejected:** Sequential `string.replace`; embedding any original value in the token; sending a truncated prompt when any entity is DENY.

---



## D-012 — Vault hashing and encryption

**Source:** Specified encryption; assumption for hash.

**Expires:** Phase 2.

**Decision:**

- `encrypted_value`: AES-256-GCM; key from `VAULT_ENCRYPTION_KEY` (32 bytes, Base64-encoded in configuration). Missing/invalid key → fail startup (or fail closed on use in tests).
- `value_hash`: HMAC-SHA256 of UTF-8 original value using a key derived from the vault key (or a separate `VAULT_HASH_KEY` if both are configured). Store hex or Base64 of the MAC, never the value.
- Lookup uses tenant + conversation + entity type + value hash.
- Expired or inactive mappings **must not** resolve.

**Rejected:** SHA-256 without a key; storing plaintext “for tests”; reversible tokens.

---



## D-013 — Re-identification authorization

**Source:** Interpretation (FR-009, sequence flow 3, chat example vs production API note).

**Expires:** Phase 7 (behavior) and Phase 8 (claim issuance).

**Decision:**

- The LLM and the Ollama adapter **never** call `TokenVault.resolve`.
- `POST /chat` returns **re-identified** text only when JWT `reidentify === true`.
- If `reidentify` is false or absent, chat returns the **inspected, still-tokenized/redacted** text (plus privacy counts).
- `POST /privacy/analyze` never re-identifies; it returns types, actions, opaque tokens, and sanitized text only.
- Vault resolve is only invoked from gateway response-privacy code after this check.

The chat example in `docs/06-api-specification.md` that shows `"John Smith reported..."` is the **authorized** demo path, not the default for every caller.

**Rejected:** Re-id for every authenticated user; letting model output or a prompt instruction trigger resolve; a public resolve endpoint.

---



## D-014 — Response inspection (new PII)

**Source:** Assumption (FR-008 requires it; action unspecified).

**Expires:** Phase 7.

**Decision:**

1. Scan the LLM response for known tokens in **this** tenant + conversation. Do not resolve yet.
2. Run the **same detector** on the response text.
3. Newly detected sensitive spans (not already a known token) are **REDACT**ed in the user-facing response. Do **not** insert LLM-invented values into the vault.
4. If detection on the response fails, **fail closed** for the user response: return a controlled error; do not return the raw LLM text. The inbound prompt was already protected; do not “fail open” on the way out.
5. Then apply D-013 for known tokens only.

**Rejected:** Skipping response detection; writing hallucinated PII into `token_mapping`; returning raw model text when the detector is down.

---



## D-015 — Detector contract

**Source:** Assumption (independent Python service; API unspecified).

**Expires:** Phase 3.

**Decision:** Detector is HTTP JSON:

- `POST /detect` `{ "text": "..." }` → `{ "entities": [ { "type", "start", "end", "score"? } ] }`
- Offsets are Unicode code-point offsets into the submitted string (document in detector tests; gateway and detector must agree).
- Gateway authenticates **to** the detector with a shared secret from configuration (`PRIVACY_DETECTOR_TOKEN`) in `Authorization: Bearer`. Detector rejects missing/invalid credentials.
- Detector **must not** log request text, entity values, or raw bodies. Log only counts, types, latency, correlation id.
- Detector timeout is configured on the gateway. Timeout/5xx/invalid schema → fail closed; **do not** call Ollama.

**Rejected:** Unauthenticated detector; detector writing plaintext traces; gateway continuing on detector failure.

---



## D-016 — Ollama request-boundary tests

**Source:** Specified (`docs/12`) plus assumption on evasion.

**Expires:** Phase 6.

**Decision:** The critical test captures the HTTP body actually sent to Ollama (or a test double that records that body).

Assertions:

- Configured protected originals are absent as UTF-8 substrings.
- Also absent after JSON decoding and after decoding `\uXXXX` escapes in that body.

This is still not a full homoglyph suite. Homoglyph/NFKC tests may be added in Phase 10; they are not required to start Phase 6.

**Rejected:** Asserting only on an in-memory `sanitizedPrompt` field; a mock that never serializes the provider request.

---



## D-017 — Demo usefulness vs privacy

**Source:** Assumption (`docs/15` dual proof).

**Expires:** Phase 10.

**Decision:** If the local model cannot summarize well over tokens, **do not** weaken TOKENIZE/REDACT/MASK to make the demo look smarter.

Evidence required for MVP:

1. Privacy: D-016 plus fail-closed detector outage.
2. Usefulness: the model returns a coherent summary **over tokens** (entities referred to as tokens is acceptable).

If (2) fails with the configured model, record it as a **model limitation** in Phase 10; do not punch a hole in the pipeline.

---



## D-018 — Logging and metrics

**Source:** Specified outcome; assumption for mechanism.

**Expires:** Phase 8 (logging checks in Phase 10).

**Decision:** Structured logs may include correlation id, tenant id, user id, conversation id, entity **types**, action names, counts, HTTP status, latency.

Logs, metrics labels, and audit `metadata` **must not** include: message bodies, sanitized prompts, tokens’ mapped values, emails, names, account numbers, JWTs, encryption keys.

Phase 10 must include an automated check that fixture PII strings do not appear in captured log output for the e2e path.

---



## Ratification

- [x] D-001–D-018 accepted as written
- [ ] Accepted with amendments (list IDs and deltas below)

Amendments:

```text
(none)
```

Phase 1 may proceed under `docs/14-implementation-plan.md` when explicitly instructed. Do not start Phase 1 in the same step as ratification unless asked.