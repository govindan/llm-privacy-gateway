# LLM Privacy Gateway — Specification & PoC (v1.0)

End-to-end proof of concept for a privacy-preserving LLM gateway: sensitive values are detected and transformed **before** local inference (Ollama), with optional authorized re-identification after response inspection.

**Governing spec:** `docs/`. Where documents conflict with accepted decisions, **`docs/16-decision-record.md` (D-001–D-018)** wins.

## Project status

| | |
|--|--|
| **Overall** | Runnable **PoC** (local Docker + Ollama). Not production-hardened. |
| **MVP acceptance** | Criteria in `docs/15-acceptance-criteria.md` are **not fully signed off** until Phase 10 completes. |

**Done**

- Privacy pipeline: detect → policy → tokenize/sanitize → Ollama → response inspection
- Encrypted token vault, tenant-scoped mappings, conversation + audit (metadata only)
- JWT on `/api/v1`, optional dev token mint for the demo UI
- Four-pane React UI at `http://localhost:3001`
- Unit/integration tests for backend and detector (see `README` build section)

**Not started**

- **Phase 10 — End-to-end hardening** (`docs/14-implementation-plan.md`): negative paths, detector/Ollama outage, tenant isolation checks, log/PII verification, Compose validation, documentation pass

**Phase breakdown**

| Phase | Scope | Status |
|-------|--------|--------|
| 0 | Readiness | Done |
| 1 | Skeleton | Done |
| 2 | Vault + Flyway | Done (`V1`–`V3`) |
| 3 | Detector service | Done |
| 4 | Policy engine | Done |
| 5 | Sanitize / tokenize | Done |
| 6 | Ollama provider | Done |
| 7 | Response privacy | Done |
| 8 | JWT + audit | Done |
| 9 | React demo UI | Done |
| 10 | E2E hardening | **To do** |

## Quick start (Docker Compose)

1. Copy environment file (never commit `.env`):

   ```bash
   cp .env.example .env
   ```

2. Start stack (Docker Desktop must be running):

   ```bash
   docker compose up --build -d postgres privacy-detector backend frontend
   ```

   The `ollama` service is defined in `docker-compose.yml` but optional. For a **Mac host Ollama** install, set in `.env`:

   ```bash
   OLLAMA_BASE_URL=http://host.docker.internal:11434
   OLLAMA_MODEL=<a model you have pulled locally>
   ```

   For **Compose Ollama**, use `OLLAMA_BASE_URL=http://ollama:11434` and include `ollama` in the `up` command; pull a model inside that container.

3. Open the demo UI: **http://localhost:3001** (mapped from frontend container port 80).

4. In the UI: **Mint demo token** (requires `DEV_TOKEN_ENABLED=true` in `.env`), then **Analyze only** or **Analyze + Chat**.

Public health endpoints:

| Service | URL |
|---------|-----|
| Gateway | `GET http://localhost:8080/actuator/health` |
| Detector | `GET http://localhost:8000/health` |
| Frontend | `GET http://localhost:3001/health` |

## API surface (`/api/v1`)

All routes below require `Authorization: Bearer <JWT>` except health checks and (when enabled) dev token mint.

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/chat` | Sanitize prompt → Ollama → response inspection / re-id |
| `POST` | `/privacy/analyze` | Detect + transform only (no LLM) |
| `GET` | `/audit/conversations/{conversationId}` | Audit metadata (no plaintext PII) |
| `POST` | `/dev/token` | Local JWT mint if `DEV_TOKEN_ENABLED=true` |

JWT claims: `sub`, `tenant_id`, `reidentify` (boolean). Details: `docs/06-api-specification.md`, D-006/D-013 in the decision record.

The UI calls the gateway through nginx (`frontend/nginx.conf` proxies `/api` → backend).

## Repository layout

| Path | Role |
|------|------|
| `backend/` | Spring Boot 3.4 / Java 21 gateway |
| `privacy-detector/` | Python 3.12 FastAPI detector |
| `frontend/` | React 18 + Vite demonstration UI |
| `backend/src/main/resources/db/migration/` | Flyway `V1`–`V3` |
| `docker-compose.yml` | Compose services (frontend on host **3001**) |
| `.env.example` | Required environment variables |
| `docs/` | Product, architecture, API, tests, acceptance |

## Build and test locally

Backend (Java 21; host JDK 11 is not sufficient):

```bash
docker run --rm -v "$PWD/backend":/workspace -w /workspace \
  maven:3.9.9-eclipse-temurin-21 mvn -B test
```

Detector:

```bash
cd privacy-detector && python3 -m venv .venv && .venv/bin/pip install -r requirements-dev.txt && .venv/bin/pytest
```

Frontend:

```bash
cd frontend && npm install && npm run build
```

Dev server (proxies `/api` to `localhost:8080`):

```bash
cd frontend && npm run dev
```

## Using this package with Cursor

1. Read `CURSOR_START_PROMPT.md` for the phased workflow (optional for Cursor users).
2. Do not treat Phase 10 acceptance as complete until `docs/14-implementation-plan.md` Phase 10 is implemented.

Local Cursor rule files under `.cursor/` are gitignored and are not required to build or run the PoC.

## Security notes for GitHub

- `.gitignore` excludes `.env`, keys, `backend/target/`, `frontend/node_modules/`, and virtualenvs.
- Replace all placeholders in `.env.example` before running; use strong `JWT_SECRET` (≥ 32 bytes) and unique detector/vault secrets in any shared environment.
- Set `DEV_TOKEN_ENABLED=false` outside trusted local demos.

## License and contributing

- [LICENSE](LICENSE) — MIT (update the copyright line if your organization requires a different holder or license).
- [CONTRIBUTING.md](CONTRIBUTING.md) — how to propose changes and run tests.
