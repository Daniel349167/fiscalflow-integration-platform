package dev.daniel.fiscalflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    private static final int MAX_ATTEMPTS = 3;

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected OutboxEvent() {
    }

    private OutboxEvent(UUID documentId, Instant now) {
        this.id = UUID.randomUUID();
        this.aggregateId = documentId;
        this.eventType = "DOCUMENT_SUBMISSION_REQUESTED";
        this.status = OutboxStatus.PENDING;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public static OutboxEvent submissionRequested(UUID documentId, Instant now) {
        return new OutboxEvent(documentId, now);
    }

    public void markPublished(Instant now) {
        status = OutboxStatus.PUBLISHED;
        processedAt = now;
        lastError = null;
    }

    public boolean markRetry(String error, Instant now) {
        attemptCount++;
        lastError = error;
        if (attemptCount >= MAX_ATTEMPTS) {
            status = OutboxStatus.DEAD_LETTER;
            processedAt = now;
            return true;
        }
        status = OutboxStatus.RETRY;
        nextAttemptAt = now.plus(Duration.ofSeconds(1L << attemptCount));
        return false;
    }

    public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public OutboxStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
}
