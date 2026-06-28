package dev.daniel.fiscalflow.infrastructure.provider;

import dev.daniel.fiscalflow.domain.FiscalDocument;
import dev.daniel.fiscalflow.domain.IntegrationProvider;
import org.springframework.stereotype.Component;

@Component
public class DemoFastProviderAdapter implements ProviderAdapter {
    @Override
    public IntegrationProvider provider() {
        return IntegrationProvider.DEMO_FAST;
    }

    @Override
    public ProviderResult submit(FiscalDocument document, int previousAttempts) {
        if (document.getExternalReference().toUpperCase().contains("RETRY") && previousAttempts < 1) {
            return ProviderResult.transientError("Simulated provider timeout; retry scheduled");
        }
        return ProviderResult.accepted("FAST-" + document.getId().toString().substring(0, 8).toUpperCase());
    }
}
