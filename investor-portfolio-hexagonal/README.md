# investor-portfolio-hexagonal

Two endpoints rebuilt as a standalone Spring Boot service using Hexagonal
(Ports and Adapters) Architecture:

- `GET /investor/getInvestorPortfolioNew` — portfolio valuation
- `GET /investor/getTaxReportsNew` — realised capital-gain (tax) report

It is a behaviour-preserving port of the endpoint in `investor-portfolio-service`. The
valuation logic was carried over line-for-line; only the *direction of dependencies* changed.
Both services read the same tables, so their responses are equivalent.

Under the `dev` profile it runs on port **8099** while the legacy service uses **8098**, so the
two can run side by side for response diffing. Prod defaults to **8090** (`${SERVER_PORT:8090}`),
which clears the earlier collision with the legacy service's 8080.

Further documentation: [ARCHITECTURE.md](ARCHITECTURE.md) ·
[CODING-GUIDELINES.md](CODING-GUIDELINES.md) · [CONTRIBUTING.md](CONTRIBUTING.md) ·
[ADRs](docs/adr/)

## Layers

```
com.hexagonal.portfolio
├── domain/                      no Spring, no JPA, no HTTP
│   ├── model/                   portfolio aggregate, holdings, cashflows, payload shapes
│   └── service/                 XIRR solver, segregated-scheme + rupee-format rules
├── application/
│   ├── port/in/                 what the outside may ask for (use cases)
│   ├── port/out/                what this service needs from the outside (18 load ports)
│   └── service/                 use-case implementations — depend only on ports
└── adapter/
    ├── in/web/                  REST controller (driving)
    └── out/persistence/         JPA entities, Spring Data repos, port impls (driven)
```

The dependency rule is enforced in only one direction: `adapter → application → domain`.
Verified as clean — the domain references neither `application` nor `adapter`, the
application references no `adapter`, and neither imports `jakarta.persistence`,
`org.springframework.web` or `org.springframework.data`.

## Ports

| Input port | Purpose |
|---|---|
| `GetInvestorPortfolioUseCase` | Value a portfolio as at a cut-off date |
| `GetFamilyMfPortfolioUseCase` | Family roll-up (`report_type=family`) |
| `GetFolioMasterSummaryUseCase` | Folio master summary (`report_type=folio`) |
| `ConvertToMobilePortfolioUseCase` | Mobile portfolio payload (`source=mobile`) |
| `GetInvestorTaxReportUseCase` | Scheme-wise realised capital-gain rows |
| `ConvertToCapitalGainReportUseCase` | Mobile capital-gain payload (`source=mobile`) |

Eighteen output ports cover investor lookup, CAMS/Karvy/manual transactions, SIP
registrations, CAMS/Karvy folio masters, AMFI scheme master, latest NAV, NAV history,
fund score, health-checkup rating, family mapping, the transaction-type vocabulary, the
tax-specific CAMS/Karvy reads, raw transaction types and the cost-inflation index.

Each output-port method mirrors the signature of the repository method it replaced. That is
deliberate: it let the 3,415-line valuation body move across without a single statement
changing, which is what keeps the numbers identical.

## Why the payload classes live in `domain/model`

This is a read model. The valuation produces `InvestorPortfolioResponse` (and the family /
folio / mobile shapes) as its result, and the web adapter serialises them directly rather
than remapping ~200 fields into parallel DTOs. A remapping layer would have been the larger
source of response drift, which the port was specifically meant to avoid.

## Fidelity

Every ported file was diffed against its original after normalising the intentional
dependency renames:

| File | Lines | Result |
|---|---|---|
| `InvestorPortfolioService` (valuation) | 3,415 | identical |
| `InvestorTaxReportService` (capital gain) | 1,270 | identical |
| `CapitalGainReportMapper` | 417 | identical |
| `FamilyMfPortfolioService` | 342 | identical |
| `FolioMasterSummaryService` | 264 | identical |
| `MobilePortfolioMapper` | 245 | identical |
| `getInvestorPortfolioNew` endpoint | 55 | identical |
| `getTaxReportsNew` endpoint | 22 | identical |
| 13 payload classes | 3,088 | identical |

Rounding is left exactly as the original wrote it — `DecimalFormat` formatted then
re-parsed through `Double.parseDouble` — because changing it would move the emitted figures.

One deliberate call-site change: the legacy tax service passed `PageRequest.of(0, 1)` into
`findInflationIndex` from inside the service. That `Pageable` argument now lives in
`InflationIndexPersistenceAdapter` so the application layer stays free of Spring Data types.
The query, its ordering and the one-row cap are unchanged.

Grab-bag utilities (`MfboxUtils`, `MyMFBoxUtils`, `TrackerUtils`, `TransactionDataUtils`) were
narrowed to only the methods these two endpoints reach — each extracted verbatim and diffed —
because the originals drag in unrelated web and mail types. Repositories were likewise trimmed
to the methods actually called, with every `@Query` copied unchanged.

JPA entities and their domain counterparts were checked for field parity (the domain models
are the entities minus persistence annotations), so the `BeanUtils` copy in
`PersistenceMapper` is lossless.

## Build and run

Credentials are not in the repository. Supply them first — the application refuses to start
without them:

```bash
cp .env.example .env      # then fill in the four DB_* values
mvn spring-boot:run       # dev profile, port 8099
```

```bash
mvn clean verify          # tests + ArchUnit, Checkstyle, PMD, SpotBugs, Spotless, JaCoCo
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for the quality gates and the ratcheted baselines.

## Known issues

- **The committed credentials are still readable in this repository's public git history.**
  They have been removed from the working tree, but that does not unpublish them. Both database
  users and the JWT key must be rotated, and the history rewritten. See
  [ADR-0006](docs/adr/0006-externalised-secrets.md).
- **`InvestorPortfolioService` (3,400 lines) and `InvestorTaxReportService` are effectively
  untested** — 0.1% instruction coverage between them, and 82% of everything still uncovered.
  This is why the JaCoCo bundle gate sits at 20% rather than the 80% target.
- **`FamilyMfPortfolioService` and `MobilePortfolioMapper` hold a static `SimpleDateFormat`**,
  which is not thread-safe; under concurrent requests it can emit a corrupted date. Found by PMD,
  not yet fixed.
- **`TransactionDataUtils` never cancels a SIP Rejection.** The branch tests the rejection's own
  transaction type against the systematic-purchase literals, so the condition can never be true.
  Pinned as-is by `TransactionDataUtilsTest`; fixing it changes emitted figures.
- Both XIRR solvers return `0` when they fail to converge, which is indistinguishable from a
  genuine 0% return.

## Changes from the original port

The valuation and tax bodies are still byte-identical to the legacy source. These changed:

- Field `@Autowired` replaced with constructor injection; service implementations are now
  package-private
- `System.out.println` / `printStackTrace` replaced with SLF4J
- Error responses are RFC 7807 `ProblemDetail` and **deliberately differ** from the legacy
  service ([ADR-0004](docs/adr/0004-error-responses-diverge.md)) — success payloads do not
- Request parameters carry Bean Validation constraints
- Credentials externalised; dead config removed
  ([ADR-0006](docs/adr/0006-externalised-secrets.md))
- Quality gates and 237 tests added

## Not yet done

The port has been verified by source diff and compiles cleanly, but it has **not been run
against the database** — no response has been captured from either service and compared. That
is the check worth doing before trusting it:

- `/getInvestorPortfolioNew` across `report_type` (unset / `family` / `folio`),
  `source=mobile`, and a few `folio_type` values
- `/getTaxReportsNew` across `option` (`equity` / `debt` / unset), `financialYear=All` vs a
  specific year, an explicit `startDate`/`endDate` range, and `source=mobile`

Call both services with the same parameters and diff the JSON.
