package dev.daniel.fiscalflow.infrastructure.persistence;

import dev.daniel.fiscalflow.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByDocumentIdOrderByOccurredAtAsc(UUID documentId);
}
