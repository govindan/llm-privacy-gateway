from __future__ import annotations

from app.detection.models import DetectedEntity
from app.detection.recognizers import Recognizer, build_default_recognizers


class DetectionEngine:
    """Runs registered recognizers. Overlap resolution is gateway Phase 5 (D-010)."""

    def __init__(self, recognizers: list[Recognizer] | None = None) -> None:
        self._recognizers = recognizers if recognizers is not None else build_default_recognizers()

    def detect(self, text: str) -> list[DetectedEntity]:
        if text is None:
            raise ValueError("text must not be None")
        entities: list[DetectedEntity] = []
        for recognizer in self._recognizers:
            entities.extend(recognizer.detect(text))
        # Stable ordering for callers; do not drop overlaps here.
        entities.sort(key=lambda e: (e.start, -e.end, e.type))
        return entities

    def entity_type_counts(self, entities: list[DetectedEntity]) -> dict[str, int]:
        counts: dict[str, int] = {}
        for entity in entities:
            counts[entity.type] = counts.get(entity.type, 0) + 1
        return counts
