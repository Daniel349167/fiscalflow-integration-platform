package dev.daniel.fiscalflow.infrastructure.provider;

public record ProviderResult(Outcome outcome, String reference, String detail) {
    public enum Outcome {
        ACCEPTED,
        REJECTED,
        TRANSIENT_ERROR
    }

    public static ProviderResult accepted(String reference) {
        return new ProviderResult(Outcome.ACCEPTED, reference, "Accepted");
    }

    public static ProviderResult rejected(String detail) {
        return new ProviderResult(Outcome.REJECTED, null, detail);
    }

    public static ProviderResult transientError(String detail) {
        return new ProviderResult(Outcome.TRANSIENT_ERROR, null, detail);
    }
}
