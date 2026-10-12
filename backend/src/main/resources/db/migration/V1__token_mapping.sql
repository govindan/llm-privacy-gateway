CREATE TABLE token_mapping (
    id              UUID         NOT NULL PRIMARY KEY,
    tenant_id       VARCHAR(64)  NOT NULL,
    conversation_id VARCHAR(128) NOT NULL,
    token_id        VARCHAR(64)  NOT NULL,
    entity_type     VARCHAR(64)  NOT NULL,
    encrypted_value TEXT         NOT NULL,
    value_hash      VARCHAR(128) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(128) NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    CONSTRAINT uq_token_mapping_scope_hash UNIQUE (tenant_id, conversation_id, entity_type, value_hash),
    CONSTRAINT uq_token_mapping_scope_token UNIQUE (tenant_id, conversation_id, token_id)
);

CREATE INDEX idx_token_mapping_tenant_conversation ON token_mapping (tenant_id, conversation_id);
CREATE INDEX idx_token_mapping_expires_at ON token_mapping (expires_at);
