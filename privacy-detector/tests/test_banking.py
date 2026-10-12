from app.detection import DetectionEngine


BANKING_TEXT = """
John Smith, customer ID CUST123456, contacted the bank from
john.smith@example.com.

He claims that account 987654321 was charged €20,000 on
12 August 2026.

Transaction reference TX998877.
""".strip()


def _types(entities) -> set[str]:
    return {e.type for e in entities}


def _slice(text: str, entity) -> str:
    # Offsets are Unicode code-point indexes (Python str indexes).
    return text[entity.start : entity.end]


def test_banking_scenario_detects_required_types():
    engine = DetectionEngine()
    entities = engine.detect(BANKING_TEXT)
    types = _types(entities)

    assert "PERSON" in types
    assert "CUSTOMER_ID" in types
    assert "EMAIL" in types
    assert "ACCOUNT_NUMBER" in types
    assert "TRANSACTION_ID" in types
    assert "DATE" in types
    assert "AMOUNT" in types

    by_type = {e.type: _slice(BANKING_TEXT, e) for e in entities}
    assert by_type["PERSON"] == "John Smith"
    assert by_type["CUSTOMER_ID"].upper() == "CUST123456"
    assert by_type["EMAIL"] == "john.smith@example.com"
    assert by_type["ACCOUNT_NUMBER"] == "987654321"
    assert by_type["TRANSACTION_ID"].upper() == "TX998877"
    assert by_type["DATE"] == "12 August 2026"
    assert "20,000" in by_type["AMOUNT"]
    assert "€" in by_type["AMOUNT"]


def test_unicode_code_point_offsets_for_euro_amount():
    text = "charged €20,000 today"
    engine = DetectionEngine()
    amounts = [e for e in engine.detect(text) if e.type == "AMOUNT"]
    assert amounts
    entity = amounts[0]
    assert text[entity.start : entity.end] == "€20,000"
    assert entity.start == text.index("€")
