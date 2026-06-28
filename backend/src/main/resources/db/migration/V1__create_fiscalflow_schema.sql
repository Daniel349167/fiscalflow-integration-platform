CREATE TABLE fiscal_documents (
    id UUID PRIMARY KEY,
    external_reference VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    customer_tax_id VARCHAR(11) NOT NULL,
    customer_name VARCHAR(160) NOT NULL,
    total_amount NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_reference VARCHAR(120),
    status_detail VARCHAR(500),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_fiscal_documents_created ON fiscal_documents (created_at DESC);
CREATE INDEX idx_fiscal_documents_status ON fiscal_documents (status);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES fiscal_documents(id) ON DELETE CASCADE,
    event_type VARCHAR(60) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30),
    message VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_document_time ON audit_events (document_id, occurred_at);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL REFERENCES fiscal_documents(id) ON DELETE CASCADE,
    event_type VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_due ON outbox_events (status, next_attempt_at);
