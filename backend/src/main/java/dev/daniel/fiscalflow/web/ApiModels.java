package dev.daniel.fiscalflow.web;

import dev.daniel.fiscalflow.application.DocumentCommands;
import dev.daniel.fiscalflow.application.DocumentService;
import dev.daniel.fiscalflow.domain.AuditEvent;
import dev.daniel.fiscalflow.domain.DocumentType;
import dev.daniel.fiscalflow.domain.FiscalDocument;
import dev.daniel.fiscalflow.domain.IntegrationProvider;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApiModels {
    private ApiModels() {
    }

    public record CreateDocumentRequest(
            @NotBlank @Size(max = 80) String externalReference,
            @NotNull DocumentType documentType,
            @NotNull IntegrationProvider provider,
            @NotBlank @Pattern(regexp = "\\d{11}") String customerTaxId,
            @NotBlank @Size(max = 160) String customerName,
            @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency
    ) {
        DocumentCommands.CreateDocument toCommand() {
            return new DocumentCommands.CreateDocument(
                    externalReference, documentType, provider, customerTaxId,
                    customerName, totalAmount, currency
            );
        }
    }

    public record DocumentResponse(
            UUID id,
            String externalReference,
            DocumentType documentType,
            IntegrationProvider provider,
            String customerTaxId,
            String customerName,
            BigDecimal totalAmount,
            String currency,
            String status,
            String providerReference,
            String statusDetail,
            int attemptCount,
            Instant nextAttemptAt,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        static DocumentResponse from(FiscalDocument document) {
            return new DocumentResponse(
                    document.getId(), document.getExternalReference(), document.getDocumentType(),
                    document.getProvider(), document.getCustomerTaxId(), document.getCustomerName(),
                    document.getTotalAmount(), document.getCurrency(), document.getStatus().name(),
                    document.getProviderReference(), document.getStatusDetail(), document.getAttemptCount(),
                    document.getNextAttemptAt(), document.getCreatedAt(), document.getUpdatedAt(), document.getVersion()
            );
        }
    }

    public record TimelineEventResponse(
            UUID id,
            String eventType,
            String fromStatus,
            String toStatus,
            String message,
            Instant occurredAt
    ) {
        static TimelineEventResponse from(AuditEvent event) {
            return new TimelineEventResponse(
                    event.getId(), event.getEventType(), event.getFromStatus(), event.getToStatus(),
                    event.getMessage(), event.getOccurredAt()
            );
        }
    }

    public record DashboardResponse(long total, long accepted, long rejected, long retryPending, long deadLetter) {
        static DashboardResponse from(DocumentService.DashboardSnapshot snapshot) {
            return new DashboardResponse(
                    snapshot.total(), snapshot.accepted(), snapshot.rejected(),
                    snapshot.retryPending(), snapshot.deadLetter()
            );
        }
    }

    public record DocumentDetailResponse(DocumentResponse document, List<TimelineEventResponse> timeline) {
    }
}
