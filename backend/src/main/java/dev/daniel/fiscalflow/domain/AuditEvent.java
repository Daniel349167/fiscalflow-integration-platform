package dev.daniel.fiscalflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    private AuditEvent(UUID documentId, String eventType, FiscalDocumentStatus from, FiscalDocumentStatus to, String message, Instant now) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.eventType = eventType;
        this.fromStatus = from == null ? null : from.name();
        this.toStatus = to == null ? null : to.name();
        this.message = message;
        this.occurredAt = now;
    }

    public static AuditEvent record(UUID documentId, String type, FiscalDocumentStatus from, FiscalDocumentStatus to, String message, Instant now) {
        return new AuditEvent(documentId, type, from, to, message, now);
    }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public String getEventType() { return eventType; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public String getMessage() { return message; }
    public Instant getOccurredAt() { return occurredAt; }
}
