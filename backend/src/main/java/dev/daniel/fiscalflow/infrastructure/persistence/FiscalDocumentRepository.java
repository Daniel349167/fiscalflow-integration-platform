package dev.daniel.fiscalflow.infrastructure.persistence;

import dev.daniel.fiscalflow.domain.FiscalDocument;
import dev.daniel.fiscalflow.domain.FiscalDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, UUID> {
    Optional<FiscalDocument> findByIdempotencyKey(String idempotencyKey);
    List<FiscalDocument> findTop100ByOrderByCreatedAtDesc();
    long countByStatus(FiscalDocumentStatus status);
}
