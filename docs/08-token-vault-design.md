# Token Vault Design

## Purpose

Maintain a secure mapping between opaque tokens and protected original values.

Example:

```text
<<PERSON:8F72A1>> -> encrypted("John Smith")
<<EMAIL:91AC44>>  -> encrypted("john.smith@example.com")
```

## Token requirements

- Opaque.
- Cryptographically generated.
- No embedded plaintext.
- Scoped to tenant and conversation/security context.
- Expirable.
- Not directly reversible.

## Encryption

MVP:
- AES-256-GCM.
- Key supplied by environment/configuration.
- Never hard-code key.

Production extension:
- KMS
- Key Vault
- HSM
- HashiCorp Vault

## Authorization

The LLM never gets token resolution rights.

Only the gateway performs resolution after authorization.

## Token scope

Recommended MVP scope:

```text
tenant + conversation + entity/value
```

Repeated protected values within the same scope should map consistently.
