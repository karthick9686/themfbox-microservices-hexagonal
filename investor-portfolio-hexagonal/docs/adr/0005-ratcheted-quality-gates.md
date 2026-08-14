# 5. Ratcheted baselines rather than blanket suppression

**Status:** Accepted
**Date:** 2026-08-13

## Context

Adding Checkstyle, PMD, SpotBugs, Error Prone and JaCoCo to a codebase ported verbatim from a
legacy service produces thousands of findings at once. Two obvious responses are both bad:
enforce everything and hand the team a permanently red build, or set `failOnViolation=false` and
watch the reports be ignored.

The coverage target has the same shape. 80% instruction coverage on `application` and `domain` is
the agreed goal, but `application.service` sits at 11.5% because two classes totalling ~4,700
lines are untested, and they account for 82% of everything uncovered.

## Decision

Ratchet, at the finest granularity each tool supports.

- **Checkstyle** — rules that describe defects are enforced at zero. Layout and naming rules are
  omitted entirely (ADR-0003), with the reasoning in the config. Ten ported files are suppressed
  against *named checks only* in `suppressions.xml`.
- **PMD** — `excludeFromFailureFile` lists 12 classes against *named rules only*.
- **SpotBugs** — enforced at High threshold, zero findings. Lombok-generated members excluded by
  package and pattern.
- **Error Prone / NullAway** — warnings, not errors.
- **JaCoCo** — hard 80% gates on the packages that already meet it; a 20% bundle floor marked
  `TARGET: 0.80` for the rest.

## Consequences

**Good.** The build is green and meaningful on the same day. A new violation of any *other* rule
in a baselined file still fails, and a new file must be clean from the start. The gates tighten by
deleting lines rather than by a rewrite.

**Bad.** The baselines need active burn-down or they become permanent. Each file says so and
carries instructions; CONTRIBUTING.md repeats it.

**Non-negotiable.** Baselines only shrink. Adding an entry to make a new change pass converts a
ratchet into a rubber stamp.

**Related finding.** Lombok generates ~58,000 instructions of accessors — half the codebase by
that measure. `lombok.config` sets `addLombokGeneratedAnnotation` so JaCoCo filters them.
Without it, the only route to 80% is asserting `setX`/`getX` round-trips, which moves the number
without testing anything.
