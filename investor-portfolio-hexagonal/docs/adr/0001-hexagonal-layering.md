# 1. Hexagonal layering, enforced by a fitness test

**Status:** Accepted
**Date:** 2026-08-13

## Context

Two endpoints were ported from `investor-portfolio-service` into Ports and Adapters form. The
layering was correct on arrival but held by convention only — nothing failed if a future change
pointed the domain at Spring or let an application service reach past its ports into a JPA
repository.

A standards review also asserted the domain was framework-free. Checking against a wider set of
packages than the review used, it was not: `MyMFBoxUtils` imported
`org.hibernate.internal.util.StringHelper` — ORM implementation internals, from an `internal`
package with no compatibility guarantee.

## Decision

Enforce the dependency rule with ArchUnit (`HexagonalArchitectureTest`), bound to `mvn test`.
Eleven rules covering the dependency direction, framework isolation, and port/adapter naming.

Remove the Hibernate import from the domain rather than exempt it — `StringHelper.isEmpty(s)` is
exactly `s == null || s.isEmpty()`, so inlining it is behaviour-identical.

## Consequences

**Good.** "Hexagonal" is now a property of the build rather than a claim in a README. The rules
were verified by introducing a deliberate violation and confirming three of them fired — a fitness
test that has never been seen to fail proves nothing.

**Accepted cost.** Two exemptions are encoded in the test with their reasoning: Jackson in
`domain/model` (ADR-0002), and Hibernate internals still present at 11 call sites in
`application/service`. The latter sit inside untested valuation code; the application layer is
therefore checked against persistence, web and Spring Data types rather than against all
frameworks. Both exemptions are visible edits to one file when the time comes to close them.
