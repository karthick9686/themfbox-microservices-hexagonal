# Architecture — investor-portfolio-hexagonal

Hexagonal (Ports and Adapters). Two read endpoints over mutual-fund portfolio data, ported from
`investor-portfolio-service` with the dependency direction inverted.

---

## The dependency rule

```
adapter  ───▶  application  ───▶  domain
```

Dependencies point inward only. The domain knows nothing about who calls it or where its data
comes from; the application layer speaks to the outside exclusively through ports it defines
itself.

This is enforced, not merely intended. `HexagonalArchitectureTest` fails the build if the domain
imports a framework type, if the application reaches into an adapter, or if the two adapters
reference each other. It was verified by deliberately introducing a violation and watching three
rules fire.

## Layout

```
com.hexagonal.portfolio
├── domain/                     no Spring, no JPA, no Hibernate, no HTTP
│   ├── model/                  portfolio aggregate, holdings, cashflows, payload shapes
│   ├── service/                XIRR solver, segregated-scheme and rupee-format rules
│   └── exception/              ErrorCode taxonomy + PortfolioException hierarchy
├── application/
│   ├── port/in/                6 use cases — what the outside may ask for
│   ├── port/out/               18 load ports — what this service needs from the outside
│   └── service/                use-case implementations, package-private
├── adapter/
│   ├── in/web/                 REST controller + GlobalExceptionHandler (driving)
│   └── out/persistence/        JPA entities, Spring Data repos, port impls (driven)
└── config/                     datasource wiring, startup validation
```

## Ports

| Inbound port | Purpose |
|---|---|
| `GetInvestorPortfolioUseCase` | Value a portfolio as at a cut-off date |
| `GetFamilyMfPortfolioUseCase` | Family roll-up (`report_type=family`) |
| `GetFolioMasterSummaryUseCase` | Folio master summary (`report_type=folio`) |
| `ConvertToMobilePortfolioUseCase` | Mobile portfolio payload (`source=mobile`) |
| `GetInvestorTaxReportUseCase` | Scheme-wise realised capital-gain rows |
| `ConvertToCapitalGainReportUseCase` | Mobile capital-gain payload (`source=mobile`) |

Eighteen outbound ports cover investor lookup, CAMS/Karvy/manual transactions, SIP registrations,
folio masters, AMFI scheme master, latest NAV, NAV history, fund score, health-checkup rating,
family mapping, the transaction-type vocabulary, the tax-specific reads, and the cost-inflation
index.

Each outbound port method mirrors the signature of the repository method it replaced. That was
deliberate: it let a 3,400-line valuation body move across without a statement changing, which is
what keeps the numbers identical.

## Request flow

```
GET /investor/getInvestorPortfolioNew
  │
  ├─ InvestorPortfolioController          validate params, normalise via checkParem
  │                                       branch on report_type / source
  ▼
  ├─ Get*UseCase                          (interface — the application boundary)
  ▼
  ├─ InvestorPortfolioService             valuation: units, cost basis, realised and
  │                                       unrealised gain, dividends, XIRR, category totals
  ▼
  ├─ Load*Port                            (interface — the infrastructure boundary)
  ▼
  └─ *PersistenceAdapter ──▶ Spring Data repository ──▶ MySQL
```

Failures leave through `GlobalExceptionHandler`, which is the only place exceptions become HTTP
responses.

## Two datasources

`mfportfolio` (primary) and `advisorkhoj_amfi` (secondary), wired by `PrimaryDataSourceConfig`
and `SecondaryDataSourceConfig`. Each owns an `EntityManagerFactory` and a transaction manager,
scoped by entity package — `entity/primary` and `entity/amfi` respectively.

## Where the payload classes live, and why

`InvestorPortfolioResponse` and its siblings sit in `domain/model` and are serialised directly to
HTTP. There is no DTO layer. This is a deliberate tradeoff — the rationale, its cost, and its
open status are in [CODING-GUIDELINES.md](CODING-GUIDELINES.md#2-no-dto-layer).

## Enforcement

| Concern | Mechanism |
|---|---|
| Dependency direction | `HexagonalArchitectureTest` — 11 ArchUnit rules |
| Framework isolation | Same, with documented Jackson exemption |
| Port/adapter naming | Same |
| Implementation visibility | Same, plus `ApplicationServiceProxyTest` for the proxying it relies on |
| Coding conventions | Checkstyle, PMD, SpotBugs, Error Prone (see CODING-GUIDELINES) |
| Test coverage | JaCoCo, per-package gates |

## Known gaps

- **`InvestorPortfolioService` (3,400 lines) and `InvestorTaxReportService` are effectively
  untested** — 0.1% instruction coverage between them. This is the largest risk in the codebase
  and the reason the bundle coverage gate sits at 20% rather than 80%.
- **The legacy response-diff has never been run.** Fidelity is asserted by source diff only. Until
  that check is done, several decisions here (ported layout, `equals` operand order) stay frozen.
- **`FamilyMfPortfolioService` and `MobilePortfolioMapper` hold a static `SimpleDateFormat`**,
  which is not thread-safe. Real defect, found by PMD, not yet fixed.
