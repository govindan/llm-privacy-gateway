# Privacy Policy Model

Policies are configuration, not hard-coded business logic.

Example:

```json
{
  "policyId": "BANKING_STANDARD",
  "rules": {
    "PERSON": "TOKENIZE",
    "EMAIL": "TOKENIZE",
    "PHONE": "TOKENIZE",
    "ACCOUNT_NUMBER": "TOKENIZE",
    "CARD_NUMBER": "REDACT",
    "IBAN": "TOKENIZE",
    "ADDRESS": "TOKENIZE",
    "DATE_OF_BIRTH": "TOKENIZE",
    "CUSTOMER_ID": "TOKENIZE",
    "TRANSACTION_ID": "TOKENIZE",
    "AMOUNT": "ALLOW"
  }
}
```

## Evaluation

Detection
→ classification/entity type
→ policy lookup
→ action
→ transformation

## Confidence

Detectors should expose confidence where available.

Policies may define a threshold. Below-threshold entities can be routed to a configurable fallback such as REVIEW or REDACT.

## Future model

The policy model may evolve from entity-based rules to data classification and attribute-based access control.
