# Data Model

Use PostgreSQL with migrations.

## token_mapping

```text
id
tenant_id
conversation_id
token_id
entity_type
encrypted_value
value_hash
created_at
expires_at
created_by
status
```

`encrypted_value` must never contain plaintext.

`value_hash` may support lookup/correlation without storing plaintext.

## policy

```text
id
tenant_id
name
description
version
status
created_at
updated_at
```

## policy_rule

```text
id
policy_id
entity_type
action
priority
```

## conversation

```text
id
tenant_id
created_by
created_at
updated_at
status
```

## audit_event

```text
id
tenant_id
conversation_id
timestamp
user_id
operation
policy_id
entity_type
action
success
metadata
```

Audit metadata must not contain plaintext sensitive values.

## Retention

Token mappings have configurable TTL. Expired mappings must not be resolvable.

## Tenant isolation

All security-sensitive records must be tenant-scoped and queries must enforce tenant boundaries.
