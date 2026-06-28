package dev.daniel.fiscalflow.application;

import dev.daniel.fiscalflow.domain.OutboxStatus;
import dev.daniel.fiscalflow.infrastructure.persistence.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.outbox-scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {
    private final OutboxEventRepository outbox;
    private final OutboxJobExecutor executor;

    public OutboxScheduler(OutboxEventRepository outbox, OutboxJobExecutor executor) {
        this.outbox = outbox;
        this.executor = executor;
    }

    @Scheduled(fixedDelayString = "${app.outbox-delay-ms:750}")
    public void dispatchDueEvents() {
        outbox.findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(OutboxStatus.PENDING, OutboxStatus.RETRY), Instant.now()
                )
                .forEach(event -> executor.process(event.getId()));
    }
}
