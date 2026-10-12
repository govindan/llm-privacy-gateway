# Functional Requirements

## FR-001 Authentication

The gateway shall authenticate API callers using JWT for the MVP.

## FR-002 Sensitive-data detection

Detect at minimum:

- PERSON
- EMAIL
- PHONE
- CARD_NUMBER
- IBAN
- ACCOUNT_NUMBER
- CUSTOMER_ID
- TRANSACTION_ID
- ADDRESS
- DATE_OF_BIRTH
- GOVERNMENT_ID
- AMOUNT
- IP_ADDRESS

Detection shall support rule/regex recognition and an extensible recognizer model. NER may be used where practical.

## FR-003 Policy actions

Support:

- ALLOW
- TOKENIZE
- MASK
- REDACT
- DENY

## FR-004 Tokenization

Create opaque tokens and maintain a secure mapping to the original value.

## FR-005 Deterministic relationship preservation

Within a defined conversation/security scope, repeated occurrences of the same protected value should resolve to the same token.

## FR-006 Prompt sanitization

Construct the sanitized prompt without mutating original entity offsets incorrectly.

## FR-007 LLM inference

Send only the sanitized request to Ollama.

## FR-008 Response inspection

Inspect LLM responses for known tokens and newly generated sensitive information.

## FR-009 Re-identification

Resolve tokens only when the caller has the required authorization.

## FR-010 Audit

Record security-relevant operations without plaintext protected values.

## FR-011 Debug analysis

Provide a privacy analysis endpoint that detects and transforms data without invoking the LLM.

## FR-012 UI

Provide a demonstration UI showing original input, detected entities, sanitized prompt, LLM response and authorized final response.

## FR-013 Fail closed

If privacy protection cannot be established, do not send the original request to the LLM.
