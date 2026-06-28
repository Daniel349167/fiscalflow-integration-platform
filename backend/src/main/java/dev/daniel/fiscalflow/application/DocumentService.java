package dev.daniel.fiscalflow.application;

import dev.daniel.fiscalflow.domain.AuditEvent;
import dev.daniel.fiscalflow.domain.FiscalDocument;
import dev.daniel.fiscalflow.domain.FiscalDocumentStatus;
import dev.daniel.fiscalflow.domain.OutboxEvent;
import dev.daniel.fiscalflow.infrastructure.persistence.AuditEventRepository;
import dev.daniel.fiscalflow.infrastructure.persistence.FiscalDocumentRepository;
import dev.daniel.fiscalflow.infrastructure.persistence.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {
    private final FiscalDocumentRepository documents;
    private final AuditEventRepository audits;
    private final OutboxEventRepository outbox;
    private final Clock clock;

    @Autowired
    public DocumentService(
            FiscalDocumentRepository documents,
            AuditEventRepository audits,
            OutboxEventRepository outbox
    ) {
        this(documents, audits, outbox, Clock.systemUTC());
    }

    DocumentService(
            FiscalDocumentRepository documents,
            AuditEventRepository audits,
            OutboxEventRepository outbox,
            Clock clock
    ) {
        this.documents = documents;
        this.audits = audits;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public FiscalDocument create(String idempotencyKey, DocumentCommands.CreateDocument command) {
        String hash = hash(command);
        return documents.findByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    if (!existing.getRequestHash().equals(hash)) {
                        throw new IdempotencyConflictException("Idempotency-Key was already used with a different payload");
                    }
                    return existing;
                })
                .orElseGet(() -> createNew(idempotencyKey, hash, command));
    }

    private FiscalDocument createNew(String idempotencyKey, String hash, DocumentCommands.CreateDocument command) {
        Instant now = clock.instant();
        FiscalDocument document = FiscalDocument.create(
                command.externalReference(), idempotencyKey, hash, command.documentType(), command.provider(),
                command.customerTaxId(), command.customerName(), command.totalAmount(), command.currency(), now
        );
        documents.save(document);
        audits.save(AuditEvent.record(
                document.getId(), "DOCUMENT_CREATED", null, document.getStatus(),
                "Document created with an idempotent request", now
        ));
        return document;
    }

    @Transactional
    public FiscalDocument submit(UUID id) {
        FiscalDocument document = get(id);
        Instant now = clock.instant();
        FiscalDocumentStatus previous = document.queue(now);
        outbox.save(OutboxEvent.submissionRequested(id, now));
        audits.save(AuditEvent.record(
                id, "SUBMISSION_QUEUED", previous, document.getStatus(),
                "Document and outbox command committed atomically", now
        ));
        return document;
    }

    @Transactional
    public FiscalDocument retry(UUID id) {
        return submit(id);
    }

    @Transactional(readOnly = true)
    public FiscalDocument get(UUID id) {
        return documents.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<FiscalDocument> list() {
        return documents.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> timeline(UUID id) {
        if (!documents.existsById(id)) {
            throw new DocumentNotFoundException(id);
        }
        return audits.findByDocumentIdOrderByOccurredAtAsc(id);
    }

    @Transactional(readOnly = true)
    public DashboardSnapshot dashboard() {
        long total = documents.count();
        return new DashboardSnapshot(
                total,
                documents.countByStatus(FiscalDocumentStatus.ACCEPTED),
                documents.countByStatus(FiscalDocumentStatus.REJECTED),
                documents.countByStatus(FiscalDocumentStatus.RETRY_PENDING),
                documents.countByStatus(FiscalDocumentStatus.DEAD_LETTER)
        );
    }

    private static String hash(DocumentCommands.CreateDocument command) {
        String canonical = String.join("|",
                command.externalReference().trim(),
                command.documentType().name(),
                command.provider().name(),
                command.customerTaxId().trim(),
                command.customerName().trim(),
                command.totalAmount().stripTrailingZeros().toPlainString(),
                command.currency().trim().toUpperCase()
        );
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record DashboardSnapshot(long total, long accepted, long rejected, long retryPending, long deadLetter) {
    }
}
