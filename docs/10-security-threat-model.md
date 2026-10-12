# Security Threat Model

| Threat | Impact | Primary mitigation |
|---|---|---|
| PII sent to LLM | High | Pre-LLM detection + policy + transformation |
| PII in logs | High | Structured logging + log sanitization |
| Vault compromise | High | AES-GCM + access controls + key separation |
| Token guessing | Medium | Cryptographically random opaque tokens |
| Unauthorized re-identification | High | Explicit authorization |
| Prompt injection | High | LLM has no vault/tool access |
| LLM generates PII | High | Response inspection |
| Detector unavailable | High | Fail closed |
| Cross-tenant access | High | Tenant-scoped persistence and authorization |
| Key compromise | Critical | Externalized key; future KMS/HSM |
| Sensitive metrics | Medium | No sensitive values in labels |
| Raw request persistence | High | Avoid plaintext request storage |

## Security invariants

1. Original protected values do not cross the LLM boundary when policy requires protection.
2. LLM cannot query the vault.
3. Re-identification is authorized.
4. Privacy failure blocks inference.
5. Sensitive values are not written to logs.
