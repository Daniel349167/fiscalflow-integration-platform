# ADR 0003: Simulated provider boundary

## Status

Accepted.

## Context

Real electronic-invoicing providers require contracts, credentials, certificates
and jurisdiction-specific compliance. None should be embedded in a public
portfolio repository.

## Decision

Ship deterministic `DEMO_FAST` and `DEMO_STRICT` adapters. They model acceptance,
business rejection, transient failure, retry and dead-letter behavior without
contacting SUNAT, an OSE, a PSE or a former employer.

## Consequences

- The repository is safe to run and review publicly.
- Integration architecture can be tested end to end.
- It is not a certified invoicing product and cannot issue valid tax documents.
