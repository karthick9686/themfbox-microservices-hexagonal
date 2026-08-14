# 3. Preserve ported formatting and field names

**Status:** Accepted, time-limited
**Date:** 2026-08-13

## Context

The service's fidelity claim rests on a line-for-line source diff against
`investor-portfolio-service`: `InvestorPortfolioService` (3,415 lines), `InvestorTaxReportService`
(1,270) and 13 payload classes were each diffed and found identical. The runtime response-diff
between the two services **has not been run**.

Running Google's Checkstyle ruleset over the codebase reports 13,888 violations across 13,777
lines. Of those, ~10,900 are layout and ~2,476 are the snake_case names that mirror database
columns. Roughly 3% describe anything that could misbehave.

## Decision

Preserve the ported layout and field names until the response-diff has been run.

- Checkstyle: layout and naming checks omitted, with the reasoning in the config file
- Spotless: `ratchetFrom HEAD`, and **without** `google-java-format` — only imports and
  whitespace, never statement positions
- PMD: layout rules baselined per class

## Consequences

**Good.** The diff evidence stays usable until it has actually been cashed in. A formatter run
would destroy it in one commit, before the check it supports has been performed.

**Bad.** New code and ported code do not look alike, and contributors must read
CODING-GUIDELINES.md to know which is which.

**Expiry.** Once the response-diff is complete and the numbers are trusted, the legacy source
stops being the reference. At that point: enable `google-java-format` in Spotless, re-enable the
layout checks in Checkstyle, and delete the layout entries from the PMD baseline. The
snake_case exception outlives this one — it is bound to the API contract (ADR-0002), not to the
diff.
