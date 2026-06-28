package dev.daniel.fiscalflow.domain;

public enum FiscalDocumentStatus {
    DRAFT,
    QUEUED,
    PROCESSING,
    RETRY_PENDING,
    ACCEPTED,
    REJECTED,
    DEAD_LETTER
}
