from __future__ import annotations

import re
from abc import ABC, abstractmethod

from app.detection.models import DetectedEntity


class Recognizer(ABC):
    """Extensible sensitive-data recognizer (rule/regex MVP)."""

    entity_type: str

    @abstractmethod
    def detect(self, text: str) -> list[DetectedEntity]:
        raise NotImplementedError


class RegexRecognizer(Recognizer):
    def __init__(
        self,
        entity_type: str,
        pattern: str | re.Pattern[str],
        score: float = 0.9,
        flags: int = 0,
    ) -> None:
        self.entity_type = entity_type
        self._pattern = re.compile(pattern, flags) if isinstance(pattern, str) else pattern
        self._score = score

    def detect(self, text: str) -> list[DetectedEntity]:
        entities: list[DetectedEntity] = []
        for match in self._pattern.finditer(text):
            if match.lastindex:
                start, end = match.start(1), match.end(1)
            else:
                start, end = match.start(), match.end()
            if start == end:
                continue
            entities.append(
                DetectedEntity(
                    type=self.entity_type,
                    start=start,
                    end=end,
                    score=self._score,
                )
            )
        return entities
