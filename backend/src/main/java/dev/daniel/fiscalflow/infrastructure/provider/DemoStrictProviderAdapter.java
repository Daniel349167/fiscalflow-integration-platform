package dev.daniel.fiscalflow.infrastructure.provider;

import dev.daniel.fiscalflow.domain.FiscalDocument;
import dev.daniel.fiscalflow.domain.IntegrationProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DemoStrictProviderAdapter implements ProviderAdapter {
    private static final BigDecimal LIMIT = new BigDecimal("5000.00");

    @Override
    public IntegrationProvider provider() {
        return IntegrationProvider.DEMO_STRICT;
    }

    @Override
    public ProviderResult submit(FiscalDocument document, int previousAttempts) {
        if (document.getExternalReference().toUpperCase().contains("RETRY") && previousAttempts < 2) {
            return ProviderResult.transientError("Simulated upstream saturation; retry scheduled");
        }
        if (document.getTotalAmount().compareTo(LIMIT) > 0) {
            return ProviderResult.rejected("Demo strict provider rejects totals above PEN 5,000.00");
        }
        return ProviderResult.accepted("STRICT-" + document.getId().toString().substring(0, 8).toUpperCase());
    }
}
