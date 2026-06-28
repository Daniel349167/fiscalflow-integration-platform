package dev.daniel.fiscalflow.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FiscalDocumentTest {
    private static final Instant NOW = Instant.parse("2026-06-28T10:00:00Z");

    @Test
    void followsTheHappyPathStateMachine() {
        FiscalDocument document = document("INV-001");

        assertThat(document.queue(NOW)).isEqualTo(FiscalDocumentStatus.DRAFT);
        assertThat(document.markProcessing(NOW)).isEqualTo(FiscalDocumentStatus.QUEUED);
        assertThat(document.accept("PROVIDER-001", NOW)).isEqualTo(FiscalDocumentStatus.PROCESSING);

        assertThat(document.getStatus()).isEqualTo(FiscalDocumentStatus.ACCEPTED);
        assertThat(document.getProviderReference()).isEqualTo("PROVIDER-001");
        assertThat(document.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void rejectsAnInvalidTransition() {
        FiscalDocument document = document("INV-002");

        assertThatThrownBy(() -> document.accept("PROVIDER-002", NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not being processed");
    }

    @Test
    void movesATransientFailureToRetryPending() {
        FiscalDocument document = document("RETRY-003");
        document.queue(NOW);
        document.markProcessing(NOW);

        document.markTransientFailure("timeout", NOW.plusSeconds(2), false, NOW);

        assertThat(document.getStatus()).isEqualTo(FiscalDocumentStatus.RETRY_PENDING);
        assertThat(document.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(2));
    }

    private FiscalDocument document(String reference) {
        return FiscalDocument.create(
                reference, "key-" + reference, "hash", DocumentType.INVOICE,
                IntegrationProvider.DEMO_FAST, "20123456789", "Customer SAC",
                new BigDecimal("100.00"), "PEN", NOW
        );
    }
}
