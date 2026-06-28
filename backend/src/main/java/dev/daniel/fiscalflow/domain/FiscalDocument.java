package dev.daniel.fiscalflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "fiscal_documents")
public class FiscalDocument {
    @Id
    private UUID id;

    @Column(name = "external_reference", nullable = false, length = 80)
    private String externalReference;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IntegrationProvider provider;

    @Column(name = "customer_tax_id", nullable = false, length = 11)
    private String customerTaxId;

    @Column(name = "customer_name", nullable = false, length = 160)
    private String customerName;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FiscalDocumentStatus status;

    @Column(name = "provider_reference", length = 120)
    private String providerReference;

    @Column(name = "status_detail", length = 500)
    private String statusDetail;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected FiscalDocument() {
    }

    private FiscalDocument(
            UUID id,
            String externalReference,
            String idempotencyKey,
            String requestHash,
            DocumentType documentType,
            IntegrationProvider provider,
            String customerTaxId,
            String customerName,
            BigDecimal totalAmount,
            String currency,
            Instant now
    ) {
        this.id = Objects.requireNonNull(id);
        this.externalReference = requireText(externalReference, "externalReference");
        this.idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        this.requestHash = requireText(requestHash, "requestHash");
        this.documentType = Objects.requireNonNull(documentType);
        this.provider = Objects.requireNonNull(provider);
        this.customerTaxId = requireText(customerTaxId, "customerTaxId");
        this.customerName = requireText(customerName, "customerName");
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new IllegalArgumentException("totalAmount must be greater than zero");
        }
        this.totalAmount = totalAmount;
        this.currency = requireText(currency, "currency").toUpperCase();
        this.status = FiscalDocumentStatus.DRAFT;
        this.createdAt = Objects.requireNonNull(now);
        this.updatedAt = now;
    }

    public static FiscalDocument create(
            String externalReference,
            String idempotencyKey,
            String requestHash,
            DocumentType documentType,
            IntegrationProvider provider,
            String customerTaxId,
            String customerName,
            BigDecimal totalAmount,
            String currency,
            Instant now
    ) {
        return new FiscalDocument(
                UUID.randomUUID(), externalReference, idempotencyKey, requestHash, documentType,
                provider, customerTaxId, customerName, totalAmount, currency, now
        );
    }

    public FiscalDocumentStatus queue(Instant now) {
        if (status != FiscalDocumentStatus.DRAFT && status != FiscalDocumentStatus.DEAD_LETTER) {
            throw new IllegalStateException("Only DRAFT or DEAD_LETTER documents can be queued");
        }
        FiscalDocumentStatus previous = status;
        status = FiscalDocumentStatus.QUEUED;
        statusDetail = "Submission queued";
        nextAttemptAt = now;
        updatedAt = now;
        return previous;
    }

    public FiscalDocumentStatus markProcessing(Instant now) {
        if (status != FiscalDocumentStatus.QUEUED && status != FiscalDocumentStatus.RETRY_PENDING) {
            throw new IllegalStateException("Document is not ready for processing");
        }
        FiscalDocumentStatus previous = status;
        status = FiscalDocumentStatus.PROCESSING;
        statusDetail = "Provider adapter is processing the document";
        updatedAt = now;
        return previous;
    }

    public FiscalDocumentStatus accept(String reference, Instant now) {
        requireProcessing();
        FiscalDocumentStatus previous = status;
        status = FiscalDocumentStatus.ACCEPTED;
        providerReference = requireText(reference, "providerReference");
        statusDetail = "Accepted by the simulated provider";
        nextAttemptAt = null;
        attemptCount++;
        updatedAt = now;
        return previous;
    }

    public FiscalDocumentStatus reject(String reason, Instant now) {
        requireProcessing();
        FiscalDocumentStatus previous = status;
        status = FiscalDocumentStatus.REJECTED;
        statusDetail = requireText(reason, "reason");
        nextAttemptAt = null;
        attemptCount++;
        updatedAt = now;
        return previous;
    }

    public FiscalDocumentStatus markTransientFailure(String reason, Instant nextAttempt, boolean deadLetter, Instant now) {
        requireProcessing();
        FiscalDocumentStatus previous = status;
        status = deadLetter ? FiscalDocumentStatus.DEAD_LETTER : FiscalDocumentStatus.RETRY_PENDING;
        statusDetail = requireText(reason, "reason");
        nextAttemptAt = deadLetter ? null : Objects.requireNonNull(nextAttempt);
        attemptCount++;
        updatedAt = now;
        return previous;
    }

    private void requireProcessing() {
        if (status != FiscalDocumentStatus.PROCESSING) {
            throw new IllegalStateException("Document is not being processed");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    public UUID getId() { return id; }
    public String getExternalReference() { return externalReference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public DocumentType getDocumentType() { return documentType; }
    public IntegrationProvider getProvider() { return provider; }
    public String getCustomerTaxId() { return customerTaxId; }
    public String getCustomerName() { return customerName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public FiscalDocumentStatus getStatus() { return status; }
    public String getProviderReference() { return providerReference; }
    public String getStatusDetail() { return statusDetail; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
