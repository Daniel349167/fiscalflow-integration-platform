# Security

FiscalFlow is a portfolio sandbox. Do not use real taxpayer data, certificates,
provider credentials or production documents.

## Reporting

Report a vulnerability privately to `duretae@gmail.com`. Do not open a public
issue containing credentials or personal data.

## Production gaps

- Authentication and authorization are intentionally not enabled in the local demo.
- Secrets use local-only Compose defaults.
- Provider calls are deterministic simulations.
- Rate limiting, encryption at rest, certificate custody and compliance controls
  are outside the portfolio scope and documented in the README.
