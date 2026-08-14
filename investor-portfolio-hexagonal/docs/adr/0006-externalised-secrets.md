# 6. Credentials from the environment, with no committed defaults

**Status:** Accepted
**Date:** 2026-08-13

## Context

`application-dev.properties` and `application-prod.properties` carried plaintext database
credentials and a `jwt.secret-key`. The repository is public on GitHub and the values were pushed,
so they are readable in git history regardless of what the working tree now contains.

Two aggravating details: prod used the *dev* credentials against the *same* database host, so
there was no environment separation; and the JWT secret was identical across profiles despite no
code in this service reading it — it had been copied in along with the rest of the legacy config.

## Decision

Read credentials from the environment with **no fallback value**, in every profile including dev.
A password in a dev profile is still a password in git.

Support a gitignored `.env` for local use, resolved from both the module directory and the
repository root so the IDE and the terminal behave the same. `.env.example` is the committed
template and holds no values.

Validate presence in an `EnvironmentPostProcessor`, before any bean is created.

## Consequences

**Good.** No credential in the working tree. Missing configuration fails immediately with a
message naming the absent variables.

**Why the validator exists.** `@ConfigurationProperties` binding leaves an unresolvable
`${PLACEHOLDER}` as literal text rather than throwing, so without the check the application hands
MySQL the string `${DB_PRIMARY_USERNAME}` as a username and dies several seconds later with
`Access denied` — a message that reads like a credentials problem rather than a missing-config
one.

**Cost.** Contributors must create `.env` before first run. Documented in CONTRIBUTING.md and in
the failure message itself.

**Does not fix.** Externalising does not unpublish. **Rotation of both database users and the JWT
key, plus a git history rewrite, are still outstanding** and are the only things that end the
exposure.

**Also removed.** Config read by nothing: `eureka.*` (no spring-cloud dependency),
`pdfDir`/`fontDir`/`logoDir`/`chartDir`/`thymeleaf*` (absolute paths into another developer's
checkout), `hostDomainUrl`, `vendor.logo.url`, `custom.logo.url`, `amfi.service.base-url`, and
`jwt.secret-key` itself. The service reads exactly two custom properties: `amc.logo.url` and
`custom.server.url`.

**Also changed.** Prod port 8080 → `${SERVER_PORT:8090}`, resolving the collision with
`investor-portfolio-service`.
