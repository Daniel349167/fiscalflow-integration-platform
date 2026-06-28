package dev.daniel.fiscalflow.domain;

public enum OutboxStatus {
    PENDING,
    RETRY,
    PUBLISHED,
    DEAD_LETTER
}
