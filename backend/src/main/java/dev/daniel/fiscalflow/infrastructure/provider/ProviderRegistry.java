package dev.daniel.fiscalflow.infrastructure.provider;

import dev.daniel.fiscalflow.domain.IntegrationProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderRegistry {
    private final Map<IntegrationProvider, ProviderAdapter> adapters;

    public ProviderRegistry(List<ProviderAdapter> adapters) {
        this.adapters = new EnumMap<>(IntegrationProvider.class);
        adapters.forEach(adapter -> this.adapters.put(adapter.provider(), adapter));
    }

    public ProviderAdapter get(IntegrationProvider provider) {
        ProviderAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter registered for " + provider);
        }
        return adapter;
    }
}
