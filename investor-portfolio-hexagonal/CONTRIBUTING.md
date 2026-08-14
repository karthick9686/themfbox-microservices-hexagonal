# Contributing — investor-portfolio-hexagonal

## Getting it running

**1. Supply credentials.** They are not in the repository and the application will refuse to start
without them:

```bash
cp .env.example .env
# fill in the four DB_* values
```

`.env` is gitignored. `.env.example` is **not** — it is the committed template, so never put real
values in it. Environment variables work too and take precedence in deployed environments.

If credentials are missing you get a message naming exactly which ones, before anything else
starts. If you instead see `Access denied for user '${DB_PRIMARY_USERNAME}'`, the placeholder was
not resolved — check `.env` is in the module directory next to `pom.xml`.

**2. Run.**

```bash
mvn spring-boot:run          # dev profile, port 8099
```

`.env` is picked up from either the module directory or the repository root, so IntelliJ's Run
button and a terminal in the module both work.

**3. Verify.**

```bash
mvn clean verify             # tests + every quality gate
mvn verify -Psecurity        # + OWASP dependency scan (slow, needs an NVD API key)
```

## Before you open a PR

`mvn clean verify` must pass. It runs ArchUnit, Checkstyle, PMD, SpotBugs, Spotless, Error Prone
and JaCoCo. If a gate fails, fix the finding — do not widen the baseline.

If Spotless fails, run `mvn spotless:apply`. It only touches files you changed, and only imports
and whitespace.

## The ratchets

Three files record pre-existing debt so the build can be green today while still failing on
anything new:

| File | Covers |
|---|---|
| `config/checkstyle/suppressions.xml` | 10 ported files, named checks only |
| `config/pmd/pmd-baseline.txt` | 12 classes, named rules only |
| `pom.xml` → jacoco `check` | 20% bundle floor, marked `TARGET: 0.80` |

Every entry is scoped to one file *and* one rule, so a new violation of any other rule in those
files still fails, and a new file must be clean from the start.

**These only ever get shorter.** Adding an entry to make your change pass defeats the point. If
you genuinely cannot fix something, say why in the file — every existing entry does.

## Reading order for a new contributor

1. [ARCHITECTURE.md](ARCHITECTURE.md) — the layering and why the dependencies point inward
2. [CODING-GUIDELINES.md](CODING-GUIDELINES.md) — conventions, and the six approved exceptions
3. [docs/adr/](docs/adr/) — the decisions behind the exceptions
4. `HexagonalArchitectureTest` — the rules, executable

The exceptions matter most. Several look like mistakes and are not; at least three were nearly
"corrected" during the standards review.

## Things not to do

- **Do not add logic to `InvestorPortfolioService` or `InvestorTaxReportService`.** Together they
  are ~4,700 lines at 0.1% coverage. Add tests first.
- **Do not reformat ported files.** The line-for-line diff against `investor-portfolio-service` is
  the only evidence behind the fidelity claim, and the response-diff has not been run yet.
- **Do not rename snake_case fields** or introduce a DTO layer without team agreement — both are
  deliberate, both are documented, both change the API.
- **Do not commit secrets.** Not in properties, not in `.env.example`, not in a Java constant, not
  in a comment. The credentials that were previously committed are still readable in this
  repository's public history.

## Test conventions

JUnit 5 + Mockito + AssertJ. Name tests for the behaviour they pin, not the method they call.

When a test pins behaviour that looks wrong, comment why it is pinned rather than fixed — see
`TransactionDataUtilsTest.sipRejectionIsNeverCancelled`, which documents a real bug that cannot be
fixed until the affected class has coverage.
