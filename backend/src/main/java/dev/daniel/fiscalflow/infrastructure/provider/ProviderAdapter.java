package dev.daniel.fiscalflow.infrastructure.provider;

import dev.daniel.fiscalflow.domain.FiscalDocument;
import dev.daniel.fiscalflow.domain.IntegrationProvider;

public interface ProviderAdapter {
    IntegrationProvider provider();
    ProviderResult submit(FiscalDocument document, int previousAttempts);
}
