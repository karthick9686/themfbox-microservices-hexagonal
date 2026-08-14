# 2. Serialise domain models directly; no DTO layer

**Status:** Accepted, scheduled for revisit
**Date:** 2026-08-13

## Context

The reference template maps domain objects to `record`-based `*Response` DTOs at the web adapter.
This service does not: `InvestorPortfolioResponse`, `FamilyMfPortfolioResponse` and the other
payload classes live in `domain/model` and are serialised straight to HTTP.

The service is a read model ported for byte-for-byte response equivalence with
`investor-portfolio-service`. The payload classes total ~3,000 lines across 13 types, and the
largest carries roughly 200 fields.

## Decision

Keep the domain models as the response representation. Do not introduce a DTO layer.

## Consequences

**Good.** No remapping step means no opportunity for response drift — which is the specific risk
this port was built to avoid. The valuation produces the response shape as its natural result.

**Bad.** Jackson annotations (`@JsonProperty`, `@JsonIgnore`) appear on five `domain/model`
classes, so the domain is not strictly framework-free. The architecture fitness test exempts
Jackson explicitly and records why, rather than pretending the domain is pure.

**Bad.** The HTTP contract and the domain model are the same thing, so a field rename is an API
break. This is also why the snake_case field names cannot simply be corrected (ADR-0003).

**Revisit when:** the legacy response-diff has been run and the numbers are trusted. At that point
a DTO layer can be introduced against a verified baseline. Add `com.fasterxml.jackson..` to
`FRAMEWORK_PACKAGES` in `HexagonalArchitectureTest` at the same time, and the rule will hold the
new boundary.
