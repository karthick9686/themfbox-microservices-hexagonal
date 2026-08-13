# investor-portfolio-hexagonal

Two endpoints rebuilt as a standalone Spring Boot service using Hexagonal
(Ports and Adapters) Architecture:

- `GET /investor/getInvestorPortfolioNew` — portfolio valuation
- `GET /investor/getTaxReportsNew` — realised capital-gain (tax) report

It is a behaviour-preserving port of the endpoint in `investor-portfolio-service`. The
valuation logic was carried over line-for-line; only the *direction of dependencies* changed.
Both services read the same tables, so their responses are equivalent.

Under the `dev` profile it runs on port **8099** while the legacy service uses **8098**, so the
two can run side by side for response diffing. Note that both currently declare **8080** under
`prod` — see *Known issues* below.

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

```bash
mvn clean compile
mvn spring-boot:run
```

Datasource credentials are inherited from `application-prod.properties`, pointing at the same
`mfportfolio` and `advisorkhoj_amfi` schemas the legacy service uses.

## Known issues

- **Both this service and `investor-portfolio-service` declare `server.port=8080` under the
  `prod` profile.** Whichever starts second fails to bind. The `dev` profiles are already
  separated (8099 vs 8098); prod needs the same treatment before deployment.
- **Datasource credentials and `jwt.secret-key` are committed in plaintext** in
  `application-prod.properties`, carried over from the legacy service's configuration. They
  belong in environment variables or a secrets store.
- Debug output (`System.out.println`) and `printStackTrace` calls were carried over verbatim
  from the legacy code rather than replaced with a logger, to keep the diff-based fidelity
  check meaningful. They are stdout-only and do not affect any response.

## Not yet done

The port has been verified by source diff and compiles cleanly, but it has **not been run
against the database** — no response has been captured from either service and compared. That
is the check worth doing before trusting it:

- `/getInvestorPortfolioNew` across `report_type` (unset / `family` / `folio`),
  `source=mobile`, and a few `folio_type` values
- `/getTaxReportsNew` across `option` (`equity` / `debt` / unset), `financialYear=All` vs a
  specific year, an explicit `startDate`/`endDate` range, and `source=mobile`

Call both services with the same parameters and diff the JSON.
