package dev.daniel.fiscalflow.infrastructure.persistence;

import dev.daniel.fiscalflow.domain.OutboxEvent;
import dev.daniel.fiscalflow.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Collection<OutboxStatus> statuses,
            Instant now
    );
}
