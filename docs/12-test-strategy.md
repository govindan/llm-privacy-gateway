# Test Strategy

## Unit tests

### Detection
Test all required entity categories and representative edge cases.

### Policy
Verify every supported action:
ALLOW, TOKENIZE, MASK, REDACT, DENY.

### Tokenization
- repeated value within scope gives consistent token
- different security scope does not unnecessarily share token
- token contains no plaintext
- expiration works

### Encryption
- encrypt/decrypt round trip
- invalid ciphertext rejected
- key configuration required

### Sanitization
- offsets remain correct
- multiple entities
- overlapping entities
- Unicode/currency
- repeated values
- entity ordering

## Integration tests

- gateway to detector
- gateway to PostgreSQL
- gateway to Ollama
- authorization/re-identification
- tenant isolation

## Critical security integration test

Capture the HTTP request sent to the Ollama adapter/server.

Given:

```text
John Smith
john.smith@example.com
123456789
TX998877
```

assert that none of those original protected values appear in the Ollama request when policy says TOKENIZE.

This test must operate at the provider/request boundary, not only on an internal sanitized string.

## Fail-closed test

Make detector unavailable and assert:
- chat request fails safely
- Ollama is not called

## End-to-end banking scenario

Input:

```text
John Smith, customer ID CUST123456, contacted the bank from
john.smith@example.com.

He claims that account 987654321 was charged €20,000 on
12 August 2026.

Transaction reference TX998877.
```

Expected protected entities include:
PERSON, CUSTOMER_ID, EMAIL, ACCOUNT_NUMBER, TRANSACTION_ID and DATE.
AMOUNT is ALLOW in the sample banking policy.

Verify the complete pipeline.
