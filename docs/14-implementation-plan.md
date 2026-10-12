# Implementation Plan

Implement sequentially. Do not build the whole system in one pass.

**Repository status (2026-10-12):** Phases 0–9 are implemented. Phase 10 remains open; do not claim full MVP acceptance until Phase 10 criteria in `docs/15-acceptance-criteria.md` are verified under hardening tests.

## Phase 0 — Readiness assessment

No code.

Deliver architecture assessment, risks and unresolved decisions.

## Phase 1 — Repository and architecture skeleton

Create:
- backend skeleton
- detector skeleton
- frontend skeleton
- database migration structure
- Docker Compose skeleton
- shared configuration structure

Acceptance:
- project builds
- services have health endpoints where appropriate
- no business workflow yet

## Phase 2 — Database and encrypted token vault

Implement:
- migrations
- token mapping
- encryption service
- token lifecycle
- retention
- tenant scoping

Acceptance:
- encryption tests pass
- plaintext values are not stored
- token lifecycle tests pass

## Phase 3 — Detection service

Implement detector API and required recognizers.

Acceptance:
- representative banking data is detected
- detector is independently testable

## Phase 4 — Policy engine

Implement policy persistence and evaluation.

Acceptance:
- all actions work
- sample banking policy works

## Phase 5 — Sanitization/tokenization pipeline

Implement:
- entity overlap handling
- token creation/reuse
- sanitized text
- fail-closed behavior

Acceptance:
- critical invariant tests pass

## Phase 6 — Ollama provider

Implement provider abstraction and Ollama adapter.

Acceptance:
- sanitized prompt reaches Ollama
- original protected values do not

## Phase 7 — Response privacy

Implement:
- response inspection
- known token handling
- authorization
- controlled re-identification

Acceptance:
- authorized and unauthorized cases pass

## Phase 8 — Authentication and audit

Implement JWT and audit events.

Acceptance:
- protected APIs require authentication
- audit contains no plaintext PII

## Phase 9 — React demonstration UI

Implement four-pane privacy visualization.

Acceptance:
- user can observe original, detection, sanitized LLM input and final response

## Phase 10 — End-to-end hardening

Implement:
- negative tests
- detector outage
- Ollama outage
- tenant isolation
- logging checks
- documentation
- final Docker Compose validation

Do not move to a later phase if the current phase's acceptance criteria fail.
