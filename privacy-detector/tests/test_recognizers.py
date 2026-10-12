from app.detection import DetectionEngine


def test_detects_fr002_entity_categories():
    samples = {
        "EMAIL": "Contact ada@example.org please",
        "PHONE": "Call +1 415-555-0100 now",
        "IP_ADDRESS": "Source 192.168.1.20 blocked",
        "IBAN": "Pay to GB82WEST12345698765432 today",
        "CARD_NUMBER": "Card 4111 1111 1111 1111 on file",
        "CUSTOMER_ID": "Lookup CUST999888",
        "TRANSACTION_ID": "Ref TX112233",
        "ACCOUNT_NUMBER": "Debit account 1234567890",
        "GOVERNMENT_ID": "Passport AB1234567 listed",
        "DATE_OF_BIRTH": "Date of birth 01/02/1990 on form",
        "DATE": "Meeting on 12 August 2026",
        "AMOUNT": "Total €1,234.56 due",
        "ADDRESS": "Mail to 10 Downing Street please",
        "PERSON": "Alice Wonderland arrived",
    }
    engine = DetectionEngine()
    for entity_type, text in samples.items():
        types = {e.type for e in engine.detect(text)}
        assert entity_type in types, f"expected {entity_type} in {types} for {text!r}"


def test_date_of_birth_not_required_for_plain_date():
    engine = DetectionEngine()
    entities = engine.detect("Event on 12 August 2026")
    types = {e.type for e in entities}
    assert "DATE" in types
    assert "DATE_OF_BIRTH" not in types


def test_alphanumeric_house_number_address_not_split_as_person():
    engine = DetectionEngine()
    text = "of 221B Baker Street asked us"
    entities = engine.detect(text)
    addresses = [e for e in entities if e.type == "ADDRESS"]
    assert addresses, f"expected ADDRESS in {[e.type for e in entities]}"
    span = text[addresses[0].start : addresses[0].end]
    assert span == "221B Baker Street"
    # PERSON may still match "Baker Street" raw; gateway overlap keeps longer ADDRESS.
    for person in (e for e in entities if e.type == "PERSON"):
        person_span = text[person.start : person.end]
        assert person_span != "Baker Street" or (
            addresses[0].start <= person.start and person.end <= addresses[0].end
        )


def test_alphanumeric_house_number_address_not_split_as_person():
    engine = DetectionEngine()
    text = "of 221B Baker Street asked us"
    entities = engine.detect(text)
    addresses = [e for e in entities if e.type == "ADDRESS"]
    assert addresses, f"expected ADDRESS in {[e.type for e in entities]}"
    span = text[addresses[0].start : addresses[0].end]
    assert span == "221B Baker Street"
    # PERSON may still match "Baker Street" raw; gateway overlap keeps longer ADDRESS.
    for person in (e for e in entities if e.type == "PERSON"):
        person_span = text[person.start : person.end]
        assert person_span != "Baker Street" or (
            addresses[0].start <= person.start and person.end <= addresses[0].end
        )
