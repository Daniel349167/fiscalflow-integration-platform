# ADR 0002: Idempotency and transactional outbox

## Status

Accepted.

## Context

Clients retry requests and provider calls fail transiently. A document must not be
duplicated, and a database update must not be committed without its integration
command.

## Decision

- Require `Idempotency-Key` when creating a document.
- Store a SHA-256 hash of the canonical request. Replays return the same resource;
  reusing the key with a different payload returns HTTP 409.
- Queue submission by updating the aggregate and inserting an outbox row in the
  same database transaction.
- Process retryable errors with exponential backoff and move exhausted work to a
  dead-letter state.

## Consequences

- Client retries are safe and observable.
- No database/broker dual-write exists in this version.
- The polling worker needs row claiming before horizontally scaling workers.
