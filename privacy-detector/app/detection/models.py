from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class DetectedEntity:
    type: str
    start: int
    end: int
    score: float

    def to_dict(self) -> dict:
        return {
            "type": self.type,
            "start": self.start,
            "end": self.end,
            "score": self.score,
        }
