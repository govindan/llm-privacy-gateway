# Contributing

Thanks for your interest in the LLM Privacy Gateway PoC.

## Before you change code

1. Read `README.md` for build, run, and test commands.
2. Treat `docs/` as the governing specification, especially `docs/16-decision-record.md` for accepted decisions.
3. Do not weaken the privacy boundary (sanitize before LLM, fail closed, no plaintext PII in logs or audit).

## Development setup

- Copy `.env.example` to `.env` and use local-only secrets.
- Prefer Docker Compose for the full stack; see `docs/13-docker-deployment.md`.
- Backend tests require **Java 21** (see `README.md` for the Docker Maven command).

## Pull requests

- Keep changes focused; match existing style in the area you edit.
- Run relevant tests before opening a PR:
  - `backend`: `mvn test` (Java 21)
  - `privacy-detector`: `pytest`
  - `frontend`: `npm run build`
- Describe **what** changed and **why**, and note any spec or decision-record impact.
- Never commit `.env`, keys, or real customer data.

## Security

If you find a vulnerability, do not open a public issue with exploit details. Contact the repository maintainers through your organization’s usual security channel.

## Scope

Phase 10 (e2e hardening) in `docs/14-implementation-plan.md` is still open. Contributions that add negative tests, outage handling, tenant isolation checks, or logging verification are especially welcome.
