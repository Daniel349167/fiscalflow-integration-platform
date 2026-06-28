package dev.daniel.fiscalflow.application;

import dev.daniel.fiscalflow.domain.DocumentType;
import dev.daniel.fiscalflow.domain.IntegrationProvider;

import java.math.BigDecimal;

public final class DocumentCommands {
    private DocumentCommands() {
    }

    public record CreateDocument(
            String externalReference,
            DocumentType documentType,
            IntegrationProvider provider,
            String customerTaxId,
            String customerName,
            BigDecimal totalAmount,
            String currency
    ) {
    }
}
