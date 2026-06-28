package dev.daniel.fiscalflow.application;

import dev.daniel.fiscalflow.domain.DocumentType;
import dev.daniel.fiscalflow.domain.IntegrationProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {
    private final DocumentService service;

    public DemoDataInitializer(DocumentService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("demo-accepted", "INV-2026-001", IntegrationProvider.DEMO_FAST, new BigDecimal("1250.00"));
        seed("demo-rejected", "INV-2026-002", IntegrationProvider.DEMO_STRICT, new BigDecimal("7200.00"));
        seed("demo-retry", "RETRY-2026-003", IntegrationProvider.DEMO_FAST, new BigDecimal("840.50"));
    }

    private void seed(String key, String reference, IntegrationProvider provider, BigDecimal amount) {
        var document = service.create(key, new DocumentCommands.CreateDocument(
                reference,
                DocumentType.INVOICE,
                provider,
                "20123456789",
                "Demo Trading SAC",
                amount,
                "PEN"
        ));
        if (document.getStatus() == dev.daniel.fiscalflow.domain.FiscalDocumentStatus.DRAFT) {
            service.submit(document.getId());
        }
    }
}
