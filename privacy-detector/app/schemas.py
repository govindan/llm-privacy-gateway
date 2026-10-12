from __future__ import annotations

from pydantic import BaseModel, Field


class DetectRequest(BaseModel):
    text: str = Field(..., min_length=0)


class EntityResponse(BaseModel):
    type: str
    start: int
    end: int
    score: float | None = None


class DetectResponse(BaseModel):
    entities: list[EntityResponse]
