# Coding Guidelines — investor-portfolio-hexagonal

The conventions this service follows, and — more importantly — the places where it deliberately
departs from them.

Read the exceptions section before "fixing" anything that looks wrong. Several of the oddities
here are load-bearing, and at least three were nearly corrected by mistake during the standards
review.

---

## 1. Naming

| Element | Convention | Example |
|---|---|---|
| Inbound port (use case) | `*UseCase`, interface | `GetInvestorPortfolioUseCase` |
| Outbound port | `*Port`, interface | `LoadCamsTransactionPort` |
| Use-case implementation | `*Service`, package-private | `InvestorPortfolioService` |
| Payload transformation | `*Mapper` | `MobilePortfolioMapper` |
| Driven adapter | `*Adapter`, package-private | `CamsTransactionPersistenceAdapter` |
| Driving adapter | `*Controller` | `InvestorPortfolioController` |
| Spring Data repository | `*Repository` | `InvestorTransactionCamsRepository` |
| JPA entity | *(no suffix — see exception 3)* | `entity.primary.User` |
| Domain model | *(no suffix — see exception 3)* | `domain.model.User` |

All of these except the entity/model naming are enforced by `HexagonalArchitectureTest`.

## 2. Dependency injection

Constructor injection, `private final` fields, no `@Autowired` on fields. A single constructor
needs no `@Autowired` annotation at all.

`@Value` goes on the constructor parameter, not the field:

```java
DemoService(LoadInvestorPort loadInvestorPort, @Value("${amc.logo.url}") String amcLogoPath) {
```

Checkstyle fails the build on field injection.

## 3. Visibility

Ports are public. Implementations are not — `application/service/*` and
`adapter/out/persistence/*` are package-private, so callers bind to the port rather than the
class behind it. Enforced by `HexagonalArchitectureTest`.

Note this is not purely cosmetic: `InvestorTaxReportService` is `@Transactional`, and Spring
proxies it by subclassing. `ApplicationServiceProxyTest` pins that a package-private class is
still proxyable, because nothing else in the suite would notice if it broke.

## 4. Logging

SLF4J via Lombok's `@Slf4j`. Never `System.out.println`, never `printStackTrace()` — Checkstyle
fails the build on both.

Log parameters, don't concatenate: `log.debug("userId={}", id)`.

Exceptions go to `log.error("message", ex)` — the exception is the last argument, with no
placeholder for it.

## 5. Error handling

Throw a `PortfolioException` subclass carrying an `ErrorCode`. Never catch broadly to convert a
failure into a success-shaped response.

`GlobalExceptionHandler` is the only place that maps exceptions to HTTP. `ErrorCode` deliberately
knows nothing about status codes — that mapping belongs to the web adapter, so the domain stays
transport-agnostic.

Anything not deriving from `PortfolioException` is treated as a bug: 500, logged in full, and
reported to the caller with no stack trace, exception class, or message.

## 6. Testing

JUnit 5 + Mockito + AssertJ. `@Nested` for grouping, `@DisplayName` in prose, `@ParameterizedTest`
where a table beats repetition.

Test names state the behaviour, not the method: `returnsZeroWhenItCannotConverge`, not `testXirr`.

Where a test pins behaviour that looks wrong, say so in a comment and explain why it is pinned
rather than fixed. See `TransactionDataUtilsTest.sipRejectionIsNeverCancelled`.

---

## Approved exceptions

These are decisions, not oversights. Do not change any of them without team agreement.

### 1. snake_case field names

Domain models and entities use the database's column names verbatim — `type_id`, `client_name`,
`is_purchase_allowed`, `trxn_type_`.

**Why:** these classes are serialised directly to HTTP (see exception 2), so a field rename is an
API break. The names also make the mapping to the underlying tables obvious.

**Consequence:** Checkstyle's `MemberName`, `ParameterName`, `LocalVariableName` and
`AbbreviationAsWordInName` checks are switched off in `config/checkstyle/checkstyle.xml`. That
accounts for ~2,500 of the violations Google's ruleset would otherwise report.

### 2. No DTO layer

The web adapter serialises `domain/model` types straight to the response. There is no
`*Response` record and no mapping step.

**Why:** this is a read model. The valuation *produces* `InvestorPortfolioResponse` as its result,
and remapping ~200 fields into a parallel structure would be the single most likely source of the
response drift this port exists to avoid.

**Consequence:** Jackson annotations appear on five `domain/model` classes, which is a framework
dependency in the domain. `HexagonalArchitectureTest` exempts Jackson explicitly and documents
why. If a DTO layer is ever introduced, add `com.fasterxml.jackson..` to `FRAMEWORK_PACKAGES` and
the rule will hold the new line.

**Status:** open. The review asks the team to decide whether this is an accepted tradeoff or
scheduled debt.

### 3. Entity and domain classes share simple names

Nine names exist in both `domain/model` and the JPA entity packages: `User`, `TransactionType`,
`InvestorMasterCams`, `InvestorMasterKarvy`, `InvestorSipCams49`, `InvestorTransactionCams`,
`InvestorTransactionKarvy`, `PortfolioTransactions`, `UsersMapping`.

**Why:** the domain models are the entities minus persistence annotations, so `BeanUtils` copying
in `PersistenceMapper` is lossless and field-for-field obvious.

**Consequence:** any file touching both sides must fully-qualify one of them. The reference
template would name these `*JpaEntity`. Renaming is mechanical and safe — it is simply not done
yet.

### 4. Ported file layout is preserved

Indentation, brace placement, line length and statement-per-line in the ported files are left
exactly as they came from `investor-portfolio-service`.

**Why:** the fidelity claim in the README rests on a line-for-line diff against the legacy source,
and the response-diff has still not been run. Reformatting destroys that evidence before it has
been used.

**Consequence:** Spotless is configured with `ratchetFrom` and **without** `google-java-format` —
only imports and whitespace are normalised, never statement positions. Checkstyle's layout checks
are off. Once the response-diff is complete, this exception can be retired and Spotless promoted
to a real formatter.

### 5. `equals` operand order is not "corrected"

199 sites read `value.equalsIgnoreCase("literal")` rather than the null-safe reverse.

**Why this is not a mechanical fix:** the two forms behave differently. `value.equals(...)` throws
on a null receiver; `"literal".equals(value)` returns false. In untested valuation code, flipping
them could convert a loud crash into a silently wrong number. Each site needs to be reasoned about
against the legacy source.

**Consequence:** `EqualsAvoidNull` is baselined per-file in
`config/checkstyle/suppressions.xml`, not disabled globally.

### 6. Error responses diverge from the legacy service

Success payloads are byte-equivalent to `investor-portfolio-service`. Error payloads are not —
they are RFC 7807 `ProblemDetail` documents with a stable `code` property.

**Why:** the previous behaviour reported server bugs as HTTP 400 with an empty body, and the two
endpoints disagreed with each other.

**Consequence:** when the legacy response-diff is run, failure paths must be reviewed rather than
diffed. This is called out in `GlobalExceptionHandler`'s javadoc so it is not a surprise.

---

## Quality gates

`mvn clean verify` runs all of these. All must pass.

| Gate | Posture |
|---|---|
| ArchUnit | Zero violations — hard gate |
| Checkstyle | Zero violations — hard gate |
| PMD | Ratcheted against `config/pmd/pmd-baseline.txt` |
| SpotBugs | Zero at High threshold |
| Error Prone + NullAway | Warnings only |
| Spotless | Ratcheted from HEAD |
| JaCoCo | 80% on `domain.service`, `domain.exception`, `adapter.in.web`; 20% bundle floor |

The ratchets exist so legacy debt does not block new work while still failing the build on
anything new. **They only ever get smaller.** Do not add an entry to make your change pass — fix
the finding, or explain in the file why it cannot be fixed.
