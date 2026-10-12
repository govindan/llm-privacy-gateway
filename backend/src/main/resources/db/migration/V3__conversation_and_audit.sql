CREATE TABLE conversation (
    id          VARCHAR(128) NOT NULL,
    tenant_id   VARCHAR(64)  NOT NULL,
    created_by  VARCHAR(128) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    PRIMARY KEY (tenant_id, id)
);

CREATE TABLE audit_event (
    id               UUID         NOT NULL PRIMARY KEY,
    tenant_id        VARCHAR(64)  NOT NULL,
    conversation_id  VARCHAR(128),
    event_timestamp  TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id          VARCHAR(128) NOT NULL,
    operation        VARCHAR(64)  NOT NULL,
    policy_id        UUID,
    entity_type      VARCHAR(64),
    action           VARCHAR(32),
    success          BOOLEAN      NOT NULL,
    metadata         VARCHAR(1024),
    correlation_id   VARCHAR(64)
);

CREATE INDEX idx_audit_event_tenant_conversation
    ON audit_event (tenant_id, conversation_id, event_timestamp);
