import logging

from fastapi.testclient import TestClient

from app.main import app


def test_detect_logs_do_not_contain_plaintext(monkeypatch, caplog):
    token = "test-detector-token"
    monkeypatch.setenv("PRIVACY_DETECTOR_TOKEN", token)
    client = TestClient(app)
    secret = "john.smith@example.com"

    with caplog.at_level(logging.INFO, logger="privacy-detector"):
        response = client.post(
            "/detect",
            json={"text": f"Email {secret}"},
            headers={"Authorization": f"Bearer {token}"},
        )

    assert response.status_code == 200
    joined = " ".join(record.getMessage() for record in caplog.records)
    assert secret not in joined
    assert "entity_count=" in joined
