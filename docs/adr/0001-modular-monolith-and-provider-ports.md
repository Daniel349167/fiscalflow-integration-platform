# ADR 0001: Modular monolith with provider adapters

## Status

Accepted.

## Context

Electronic-document integrations vary by provider, but the product currently has
one team, one database and one operational boundary. Splitting every concern into
a service would add deployment and consistency costs without an ownership or
scaling reason.

## Decision

Keep one Spring Boot deployable organized around domain, application, persistence,
provider and web modules. Provider-specific behavior implements a small
`ProviderAdapter` port and is selected through a registry.

## Consequences

- Adding a provider does not modify the document workflow.
- Transactions and audit events remain simple and local.
- A slow real provider must later be isolated from database transactions.
- A service split remains possible if throughput or team ownership justifies it.
