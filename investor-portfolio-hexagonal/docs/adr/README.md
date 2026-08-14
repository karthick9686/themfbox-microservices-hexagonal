# Architecture Decision Records

Short records of decisions that are not obvious from the code, and would otherwise be re-litigated
or silently reversed.

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-hexagonal-layering.md) | Hexagonal layering, enforced by a fitness test | Accepted |
| [0002](0002-no-dto-boundary.md) | Serialise domain models directly; no DTO layer | Accepted, revisit |
| [0003](0003-preserve-legacy-formatting.md) | Preserve ported formatting and field names | Accepted, time-limited |
| [0004](0004-error-responses-diverge.md) | Error responses diverge from the legacy service | Accepted |
| [0005](0005-ratcheted-quality-gates.md) | Ratcheted baselines rather than blanket suppression | Accepted |
| [0006](0006-externalised-secrets.md) | Credentials from the environment, no committed defaults | Accepted |

Format: context, decision, consequences. Keep them short. A decision that changes gets a new ADR
superseding the old one rather than an edit.
