# API Specification

Base path: `/api/v1`

## POST /chat

Request:

```json
{
  "conversationId": "conversation-123",
  "message": "Summarize the complaint from John Smith at john@example.com."
}
```

Response:

```json
{
  "conversationId": "conversation-123",
  "response": "John Smith reported a complaint...",
  "privacy": {
    "entitiesDetected": 2,
    "entitiesTokenized": 2,
    "entitiesRedacted": 0
  }
}
```

For demonstration, the UI may separately request/display sanitized content. Production APIs must avoid exposing protected values unnecessarily.

## POST /privacy/analyze

Does not call the LLM.

Request:

```json
{
  "conversationId": "conversation-123",
  "text": "John Smith's email is john@example.com"
}
```

`conversationId` is required (D-007). The gateway creates the conversation for the JWT tenant if it does not exist.

Response includes `conversationId`, `sanitizedText`, and `entities` (type, action, optional opaque token, start/end offsets).

## GET /audit/conversations/{conversationId}

Returns audit metadata only.

## Error model

Use structured errors with:
- code
- message
- correlationId

Do not expose stack traces, secrets or sensitive values.

## POST /dev/token (local / demo only)

Not part of the production contract. Enabled only when `DEV_TOKEN_ENABLED=true`.

Request:

```json
{
  "userId": "demo-user",
  "tenantId": "demo-tenant",
  "reidentify": true
}
```

Response: `{ "accessToken": "<JWT>" }` with claims `sub`, `tenant_id`, `reidentify`.

## Security

- All `/api/v1/*` routes require `Authorization: Bearer <JWT>` except `POST /api/v1/dev/token` when dev mint is enabled.
- Health: gateway `GET /actuator/health`, detector `GET /health`, frontend `GET /health` (Compose/nginx).
- Errors use `code`, `message`, `correlationId` (see Error model above).
