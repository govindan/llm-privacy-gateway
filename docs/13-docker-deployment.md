# Docker Deployment

## MVP services

```text
frontend
backend
privacy-detector
postgres
ollama          (optional — may use host Ollama instead)
```

## Topology

```text
Browser
   |
   v
Frontend (nginx, host port 3001 -> container 80)
   |  /api/* proxied to backend
   v
Backend (:8080)
 |      |      \--> Privacy Detector (:8000)
 |
 +-------> PostgreSQL (:5432)
 |
 +-------> Ollama (Compose service or host via host.docker.internal:11434)
```

## Run

```bash
cp .env.example .env
# Edit secrets and Ollama URL/model

# Minimal stack (host Ollama on Mac):
docker compose up --build -d postgres privacy-detector backend frontend

# Full stack including Compose Ollama:
docker compose up --build -d
```

Demo UI: **http://localhost:3001**

## Published ports (default Compose)

| Service | Host port | Notes |
|---------|-----------|--------|
| frontend | 3001 | SPA + `/api` reverse proxy to backend |
| backend | 8080 | Spring Boot |
| privacy-detector | 8000 | FastAPI |
| postgres | 5432 | Dev convenience; restrict in production |
| ollama | 11434 | Only if `ollama` service is started |

## Environment variables

Set via `.env` (see `.env.example`). Required for the gateway:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
VAULT_ENCRYPTION_KEY      # Base64, 32 bytes decoded
VAULT_HASH_KEY            # Base64 HMAC key for value hashing
JWT_SECRET                # HS256, at least 32 bytes
PRIVACY_DETECTOR_URL
PRIVACY_DETECTOR_TOKEN
TOKEN_TTL_HOURS
OLLAMA_BASE_URL
OLLAMA_MODEL
DEV_TOKEN_ENABLED         # default false; true for local UI token mint
```

**Ollama URL examples:**

- Compose service: `OLLAMA_BASE_URL=http://ollama:11434`
- Mac host Ollama: `OLLAMA_BASE_URL=http://host.docker.internal:11434`

Never commit `.env` or real secrets.

## Health

| Service | Check |
|---------|--------|
| backend | `GET /actuator/health` → UP |
| privacy-detector | `GET /health` |
| frontend | `GET /health` → `UP` |
| postgres | `pg_isready` (Compose healthcheck) |
| ollama | `ollama list` (Compose healthcheck when service enabled) |

## Local-only objective

After pulling the configured Ollama model, sensitive inference should remain in the local environment for this PoC.
