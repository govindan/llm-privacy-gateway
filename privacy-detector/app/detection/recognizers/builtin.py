from __future__ import annotations

import re

from app.detection.models import DetectedEntity
from app.detection.recognizers.base import RegexRecognizer, Recognizer

# Month names for DATE / DATE_OF_BIRTH patterns
_MONTH = (
    r"(?:January|February|March|April|May|June|July|August|September|October|"
    r"November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)"
)


def _luhn_valid(digits: str) -> bool:
    if not digits.isdigit() or len(digits) < 13 or len(digits) > 19:
        return False
    total = 0
    reverse = digits[::-1]
    for i, ch in enumerate(reverse):
        n = int(ch)
        if i % 2 == 1:
            n *= 2
            if n > 9:
                n -= 9
        total += n
    return total % 10 == 0


class CardNumberRecognizer(Recognizer):
    entity_type = "CARD_NUMBER"
    _pattern = re.compile(r"(?<!\d)(?:\d[ -]*?){13,19}(?!\d)")

    def detect(self, text: str) -> list[DetectedEntity]:
        entities: list[DetectedEntity] = []
        for match in self._pattern.finditer(text):
            raw = match.group(0)
            digits = re.sub(r"\D", "", raw)
            if _luhn_valid(digits):
                entities.append(
                    DetectedEntity(
                        type=self.entity_type,
                        start=match.start(),
                        end=match.end(),
                        score=0.95,
                    )
                )
        return entities


def build_default_recognizers() -> list[Recognizer]:
    return [
        RegexRecognizer(
            "EMAIL",
            r"\b([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})\b",
            score=0.99,
        ),
        RegexRecognizer(
            "IP_ADDRESS",
            r"\b((?:\d{1,3}\.){3}\d{1,3})\b",
            score=0.85,
        ),
        RegexRecognizer(
            "PHONE",
            r"(?<!\w)(\+\d{1,3}[\s.-]?\(?\d{1,4}\)?[\s.-]?\d[\d\s.-]{5,}\d|\(\d{2,4}\)[\s.-]?\d[\d\s.-]{5,}\d|\d{3}[-.\s]\d{3}[-.\s]\d{4})",
            score=0.8,
        ),
        RegexRecognizer(
            "IBAN",
            r"\b([A-Z]{2}\d{2}[A-Z0-9]{10,30})\b",
            score=0.9,
        ),
        RegexRecognizer(
            "CUSTOMER_ID",
            r"\b(CUST\d{6,})\b",
            score=0.95,
            flags=re.IGNORECASE,
        ),
        RegexRecognizer(
            "TRANSACTION_ID",
            r"\b(TX\d{5,})\b",
            score=0.95,
            flags=re.IGNORECASE,
        ),
        RegexRecognizer(
            "ACCOUNT_NUMBER",
            r"(?i)(?:account(?:\s+number)?|acct)\s*[#:]?\s*(\d{6,18})",
            score=0.92,
        ),
        RegexRecognizer(
            "GOVERNMENT_ID",
            r"(?i)(?:ssn|national\s+id|passport)\s*[#:]?\s*([A-Z0-9-]{5,20})",
            score=0.9,
        ),
        RegexRecognizer(
            "DATE_OF_BIRTH",
            rf"(?i)(?:date\s+of\s+birth|d\.?o\.?b\.?|born(?:\s+on)?)\s*[:\-]?\s*"
            rf"(\d{{1,2}}[\/\-.]\d{{1,2}}[\/\-.]\d{{2,4}}|\d{{1,2}}\s+{_MONTH}\s+\d{{4}})",
            score=0.93,
        ),
        RegexRecognizer(
            "DATE",
            rf"\b(\d{{1,2}}\s+{_MONTH}\s+\d{{4}}|\d{{4}}-\d{{2}}-\d{{2}}|\d{{1,2}}[\/\.]\d{{1,2}}[\/\.]\d{{2,4}})\b",
            score=0.85,
            flags=re.IGNORECASE,
        ),
        RegexRecognizer(
            "AMOUNT",
            r"([€$£]\s?\d{1,3}(?:,\d{3})+(?:\.\d{2})?|[€$£]\s?\d+(?:\.\d{2})?|\b\d{1,3}(?:,\d{3})+(?:\.\d{2})?\s*(?:EUR|USD|GBP)\b)",
            score=0.9,
            flags=re.IGNORECASE,
        ),
        RegexRecognizer(
            "ADDRESS",
            # House/unit may be alphanumeric (e.g. 221B Baker Street), not digits-only.
            r"\b(\d{1,5}[A-Za-z]?\s+[A-Za-z0-9.'\-]+(?:\s+[A-Za-z0-9.'\-]+)*\s+"
            r"(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Lane|Ln|Drive|Dr)\.?)\b",
            score=0.75,
            flags=re.IGNORECASE,
        ),
        RegexRecognizer(
            "PERSON",
            r"\b([A-Z][a-z]+(?:\s+[A-Z][a-z]+)+)\b",
            score=0.7,
        ),
        CardNumberRecognizer(),
    ]
