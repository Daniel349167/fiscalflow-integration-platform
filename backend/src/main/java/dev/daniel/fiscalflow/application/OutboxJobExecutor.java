package dev.daniel.fiscalflow.application;

import dev.daniel.fiscalflow.domain.AuditEvent;
import dev.daniel.fiscalflow.domain.FiscalDocument;
import dev.daniel.fiscalflow.domain.FiscalDocumentStatus;
import dev.daniel.fiscalflow.domain.OutboxEvent;
import dev.daniel.fiscalflow.infrastructure.persistence.AuditEventRepository;
import dev.daniel.fiscalflow.infrastructure.persistence.FiscalDocumentRepository;
import dev.daniel.fiscalflow.infrastructure.persistence.OutboxEventRepository;
import dev.daniel.fiscalflow.infrastructure.provider.ProviderRegistry;
import dev.daniel.fiscalflow.infrastructure.provider.ProviderResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxJobExecutor {
    private final OutboxEventRepository outbox;
    private final FiscalDocumentRepository documents;
    private final AuditEventRepository audits;
    private final ProviderRegistry providers;
    private final Clock clock = Clock.systemUTC();

    public OutboxJobExecutor(
            OutboxEventRepository outbox,
            FiscalDocumentRepository documents,
            AuditEventRepository audits,
            ProviderRegistry providers
    ) {
        this.outbox = outbox;
        this.documents = documents;
        this.audits = audits;
        this.providers = providers;
    }

    @Transactional
    public void process(UUID eventId) {
        OutboxEvent event = outbox.findById(eventId).orElse(null);
        if (event == null || event.getStatus() == dev.daniel.fiscalflow.domain.OutboxStatus.PUBLISHED
                || event.getStatus() == dev.daniel.fiscalflow.domain.OutboxStatus.DEAD_LETTER) {
            return;
        }

        FiscalDocument document = documents.findById(event.getAggregateId())
                .orElseThrow(() -> new DocumentNotFoundException(event.getAggregateId()));
        Instant now = clock.instant();
        FiscalDocumentStatus previous = document.markProcessing(now);
        audits.save(AuditEvent.record(
                document.getId(), "PROVIDER_CALL_STARTED", previous, document.getStatus(),
                "Provider adapter " + document.getProvider() + " invoked", now
        ));

        ProviderResult result = providers.get(document.getProvider()).submit(document, event.getAttemptCount());
        switch (result.outcome()) {
            case ACCEPTED -> accept(document, event, result, now);
            case REJECTED -> reject(document, event, result, now);
            case TRANSIENT_ERROR -> retry(document, event, result, now);
        }
    }

    private void accept(FiscalDocument document, OutboxEvent event, ProviderResult result, Instant now) {
        FiscalDocumentStatus previous = document.accept(result.reference(), now);
        event.markPublished(now);
        audits.save(AuditEvent.record(
                document.getId(), "PROVIDER_ACCEPTED", previous, document.getStatus(),
                "Accepted with provider reference " + result.reference(), now
        ));
    }

    private void reject(FiscalDocument document, OutboxEvent event, ProviderResult result, Instant now) {
        FiscalDocumentStatus previous = document.reject(result.detail(), now);
        event.markPublished(now);
        audits.save(AuditEvent.record(
                document.getId(), "PROVIDER_REJECTED", previous, document.getStatus(), result.detail(), now
        ));
    }

    private void retry(FiscalDocument document, OutboxEvent event, ProviderResult result, Instant now) {
        boolean deadLetter = event.markRetry(result.detail(), now);
        FiscalDocumentStatus previous = document.markTransientFailure(
                result.detail(), event.getNextAttemptAt(), deadLetter, now
        );
        audits.save(AuditEvent.record(
                document.getId(), deadLetter ? "DEAD_LETTERED" : "RETRY_SCHEDULED",
                previous, document.getStatus(), result.detail(), now
        ));
    }
}
