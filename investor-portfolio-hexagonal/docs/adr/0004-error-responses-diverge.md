# 4. Error responses diverge from the legacy service

**Status:** Accepted
**Date:** 2026-08-13

## Context

`getInvestorPortfolioNew` wrapped its entire body in
`catch (Exception e) { e.printStackTrace(); return ResponseEntity.badRequest().build(); }`.
`getTaxReportsNew` caught nothing at all.

That combination had two effects. A genuine server fault — a null dereference in the valuation, a
dead datasource — was reported to the caller as **400 Bad Request**, blaming the client and hiding
the failure from anything watching 5xx rates. And the same internal fault produced a 400 on one
endpoint and a bare 500 on the other.

The service otherwise exists to be byte-for-byte equivalent to the legacy endpoints.

## Decision

Introduce `GlobalExceptionHandler` with RFC 7807 `ProblemDetail` responses and a typed `ErrorCode`
taxonomy, accepting that error payloads will no longer match the legacy service. Success payloads
are untouched.

`ErrorCode` carries no HTTP status — the web adapter owns that mapping, so the domain stays
transport-agnostic and the architecture rules still hold.

## Consequences

**Good.** Client errors are 4xx and server errors are 5xx. Both endpoints behave alike. No stack
trace, exception class or internal message reaches a caller — pinned by a test. Callers branch on
a stable `code` property rather than on prose.

**Bad.** The outstanding legacy response-diff can no longer be a pure zero-diff check. Failure
paths must be reviewed rather than diffed. This is stated in `GlobalExceptionHandler`'s javadoc so
whoever runs that comparison is not surprised.

**Watch for.** The catch-all originally swallowed Spring MVC's own client-error signals too, so an
unknown URL and `/favicon.ico` both returned 500 with a full stack trace at ERROR. Any exception
implementing `ErrorResponse` now keeps its own status. Regression-tested in
`GlobalExceptionHandlerTest`.
