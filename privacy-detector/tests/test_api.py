import os

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture
def detector_token(monkeypatch):
    token = "test-detector-token"
    monkeypatch.setenv("PRIVACY_DETECTOR_TOKEN", token)
    return token


@pytest.fixture
def client(detector_token):
    return TestClient(app)


def auth_headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def test_health_returns_up(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP"}


def test_detect_requires_auth(client):
    response = client.post("/detect", json={"text": "hello"})
    assert response.status_code == 401


def test_detect_rejects_invalid_token(client):
    response = client.post(
        "/detect",
        json={"text": "hello"},
        headers=auth_headers("wrong-token"),
    )
    assert response.status_code == 401


def test_detect_fails_closed_when_token_unconfigured(monkeypatch):
    monkeypatch.delenv("PRIVACY_DETECTOR_TOKEN", raising=False)
    local_client = TestClient(app)
    response = local_client.post(
        "/detect",
        json={"text": "hello"},
        headers=auth_headers("anything"),
    )
    assert response.status_code == 503
