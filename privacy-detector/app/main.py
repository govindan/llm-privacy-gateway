from __future__ import annotations

import logging
import time
import uuid

from fastapi import Depends, FastAPI, Request
from fastapi.responses import JSONResponse

from app.auth import require_detector_token
from app.detection import DetectionEngine
from app.schemas import DetectRequest, DetectResponse, EntityResponse

# Never log request bodies, entity values, or raw text (D-015).
logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
logger = logging.getLogger("privacy-detector")

app = FastAPI(title="privacy-detector", version="0.3.0")
engine = DetectionEngine()


@app.middleware("http")
async def correlation_middleware(request: Request, call_next):
    correlation_id = request.headers.get("X-Correlation-Id") or str(uuid.uuid4())
    request.state.correlation_id = correlation_id
    response = await call_next(request)
    response.headers["X-Correlation-Id"] = correlation_id
    return response


@app.get("/health")
def health() -> JSONResponse:
    return JSONResponse({"status": "UP"})


@app.post("/detect", response_model=DetectResponse, dependencies=[Depends(require_detector_token)])
def detect(payload: DetectRequest, request: Request) -> DetectResponse:
    started = time.perf_counter()
    entities = engine.detect(payload.text)
    elapsed_ms = int((time.perf_counter() - started) * 1000)
    counts = engine.entity_type_counts(entities)
    correlation_id = getattr(request.state, "correlation_id", "-")
    logger.info(
        "detect completed correlation_id=%s entity_count=%s types=%s latency_ms=%s",
        correlation_id,
        len(entities),
        sorted(counts.keys()),
        elapsed_ms,
    )
    return DetectResponse(
        entities=[
            EntityResponse(type=e.type, start=e.start, end=e.end, score=e.score) for e in entities
        ]
    )
