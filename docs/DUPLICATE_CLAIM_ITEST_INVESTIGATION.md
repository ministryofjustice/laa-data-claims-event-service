# DuplicateClaimValidatorITest — 9 Failing Tests Investigation Notes

**Status as of 2026-09-11:** Root cause of the mock-related failures identified. Fix in progress.
Branch: `DSTEW-1717` (all changes described below are currently *uncommitted* working-tree edits;
`git log` shows `HEAD == origin/main`).

## How to reproduce

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  ./gradlew :data-claims-event-service:integrationTest --tests "*DuplicateClaimValidatorITest*"
```
(Repo requires Java 25 to run Gradle itself — the `laa-spring-boot-gradle-plugin` build script
dependency needs JVM 25+. Java 21 is also installed at
`/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` but cannot run the build.)

Unit tests (`test` task, not `integrationTest`) for duplicate validation all pass (83/83) —
the problem is isolated to the `integrationTest` source set.

## The 9 failing cases

All fail in `DuplicateClaimValidatorITest.shouldMatchValidationReportForClaimsResponse`, all with
the exact same shape of assertion failure:

```
Claim <id> mismatch [new=0, existing=1]:
Only in existing: source=Data-Claims-Event-Service type=ERROR display=A duplicate claim was found within the same submission technical=null
```

Fixtures affected (all under
`data-claims-event-service/src/integrationTest/resources/responses/data-claims/get-claims/DuplicateClaimValidator/`):

| Fixture | Expected | existing (repo) | new (library) |
|---|---|---|---|
| lh-no-duplicate.json | NONE | 1 (false positive) | 0 (correct) |
| lh-duplicate.json | DUPLICATE | 1 (correct) | 0 (missed) |
| lh-disbursement-no-duplicate.json | NONE | 1 (false positive) | 0 (correct) |
| lh-disbursement-duplicate.json | DUPLICATE | 1 (correct) | 0 (missed) |
| lh-disbursement-cross-submission-duplicate.json | DUPLICATE_OTHER | 1 (correct) | 0 (missed) |
| lh-disbursement-no-cross-submission-duplicate.json | NONE | 1 (false positive) | 0 (correct) |
| cl-no-duplicate.json | NONE | 1 (false positive) | 0 (correct) |
| cl-duplicate.json | DUPLICATE | 1 (correct) | 0 (missed) |
| cl-no-cross-submission-duplicate.json | NONE | 1 (false positive) | 0 (correct) |

`shouldOnlyProduceExpectedErrorCodes` (the sibling parameterized test) passes for all fixtures —
only the strict "existing vs new" parity assertion fails.

## Test architecture (background)

`DuplicateClaimValidatorITest` extends `ClaimValidationIntegrationTestBase`, which for each claim
in a fixture computes TWO independent validation outputs and asserts they match exactly:

- **"existing"** = `SubmissionValidationContext` produced by *this repo's*
  `submissionValidationService.validateSubmission(...)`, which runs the (recently rewritten)
  `DuplicateClaimValidation` strategies in
  `validation/claim/duplicate/*` — these call the real `DataClaimsRestClient.getClaims(...)`,
  which is stubbed via MockServer.
- **"new"** = the external shared library `claims-validation-core`'s
  `ValidationService.validateClaim(mapped, null, relatedClaims)` — a migration-target validation
  engine, wired to the *same* MockServer container via its own `HttpClaimsDataProvider`/
  `DataClaimsClient` (see `ClaimValidationIntegrationTestBase`'s `ClaimsConfiguration`).

## Root cause #1 (CONFIRMED, fixing now): broad/unfiltered mock stub → false positives on "existing" side

### The branch's production change (intentional, confirmed via `git diff main`)

`DuplicateClaimValidation.java` (this repo) used to filter `submissionClaims` (the in-memory list
passed into the validator) with an explicit client-side predicate:
```java
Objects.equals(feeCode, claimToCompare.getFeeCode())
    && Objects.equals(uniqueFileNumber, claimToCompare.getUniqueFileNumber())
```
Now it calls `getDuplicateClaims(officeCode, feeCode, uniqueFileNumber, uniqueClientNumber)` →
`dataClaimsRestClient.getClaims(...)`, **trusting the API to have already filtered** by those
criteria (real `@RequestParam`s confirmed on `DataClaimsRestClient.getClaims`). Client-side, only
`filterDuplicateClaimsInSameSubmission`/`filterDuplicateClaimsInPreviousSubmission` remain, and
those check **only `submissionId` equality** (+ self-id exclusion) — no more UFN/UCN/fee-code
re-check.

**This is a legitimate design simplification, not a bug** — provided the mocked test API actually
emulates server-side filtering.

### The test bug

`ClaimValidationIntegrationTestBase.runSubmissionValidationWithClaims` (also modified this branch)
registers, for **each claim** in the fixture, a MockServer stub matching that claim's own
`office_code`/`fee_code`/`unique_file_number`/`unique_client_number` query params — but the
response body is **always the entire `claimsFixture` file** (all N claims), regardless of which
claims actually share that specific UFN/UCN:

```java
// ClaimValidationIntegrationTestBase.java (current, buggy)
stubForGetClaims(duplicateParams, claimsFixture); // <-- claimsFixture is the WHOLE file, unfiltered
```

Since claims in one fixture always share the same `submission_id` (they're one submission), and
the production code's `filterDuplicateClaimsInSameSubmission` only checks submission-id equality,
**every claim in a multi-claim fixture is treated as a duplicate of every other claim** — even in
fixtures deliberately named `*-no-duplicate.json` that contain claims with genuinely distinct
UFN/UCN values (e.g. `lh-no-duplicate.json` claims `...001`/`...002`/`...003` have UFNs
`290419/001`/`002`/`003` and distinct UCNs, but the mock still returns all three for every query).

### The fix (in progress)

In `ClaimValidationIntegrationTestBase.runSubmissionValidationWithClaims`, before stubbing the
per-claim duplicate-check response, filter the parsed claim list down to only those claims whose
`fee_code`, `unique_file_number`, and `unique_client_number` all equal the current claim's values
(this emulates what the real Data Claims API's query-param filtering would return), and stub with
*that* JSON subset instead of the raw fixture file content.

Needs a new/adjusted helper in `MockServerIntegrationTest` that can stub a `GET /claims` response
from an in-memory JSON body (e.g. `ClaimResultSet`) rather than only from a fixture file path, since
the filtered subset is fixture-derived but not itself a fixture file.

## Root cause #2 (NOT YET FIXED, deliberately deferred by user): "new" (library) side always returns 0 issues

This is **independent** of root cause #1's broad-stub bug for the *same-submission* check
specifically:

- Traced `claims-validation-core` v1.4.10 sources (via `jar tf` / `unzip -p`, non-destructively,
  no extraction to disk — cache at
  `~/.gradle/caches/modules-2/files-2.1/uk.gov.justice.laa.dstew.payments.claims.validation/claims-validation-core/1.4.10/...-sources.jar`).
- `DuplicateClaimValidation.findSameSubmissionDuplicates` (library base class) takes a **bulk,
  in-memory-only path** whenever `submissionClaims` (i.e. `relatedClaims`, always non-empty in this
  test) is non-empty — it does **not** call the mocked API at all in that case. It just filters
  siblings by valid status + an exact `feeCode`/`UFN`/`UCN` match predicate, all in memory.
- Since `relatedClaims` passed by the test genuinely contains real duplicates for fixtures like
  `lh-duplicate.json`/`cl-duplicate.json` (matching fee code + UFN + UCN), the library's in-memory
  match *should* fire — but it doesn't. This means root cause #2 is **not explained by the mock
  stub problem** and needs live debugging to pin down (e.g. breakpoint inside
  `DuplicateSameSubmissionLegalHelpValidationServiceStrategy.validateDuplicateClaims` /
  `ClaimMapper.fromClaimResponse` to inspect actual field values of `currentClaim` vs
  `relatedClaims` at runtime).
- Ruled out during investigation:
  - `Validator.appliesTo(scope)` default returns `true` for `scope == null` (runs **all**
    validators, not just "scope-agnostic" ones — the `ValidationService` javadoc wording is
    misleading here), so scope isn't silently skipping `DuplicateClaimValidator`.
  - `AreaOfLaw` enum matching between `LegalHelpDuplicateClaimValidationStrategy
    .compatibleAreaOfLaws()` and the test's `testAreaOfLaw` derivation looks consistent.
  - No uncaught exception — `getIssues()` returns successfully with an empty list (not a
    `TECHNICAL_ERROR_FEE_SCHEME_API`/`TECHNICAL_ERROR_DATA_CLAIMS_API` issue either — those would
    show up as "Only in new:" entries in the assertion failure, and none do).
- For the 2 genuine **cross-submission** fixtures
  (`lh-disbursement-cross-submission-duplicate.json` /
  `lh-disbursement-no-cross-submission-duplicate.json`), the library's
  `getDuplicateClaimsInPreviousSubmission` **does** call the mocked API via its own
  `HttpClaimsDataProvider`. Its query omits `unique_client_number` for strategies whose dedupe key
  doesn't include UCN, whereas this repo's `stubForGetClaimsFromPreviousSubmission` always includes
  a UCN param — a likely **parameter-shape mismatch** (not the "returns everything" bug), causing
  MockServer to not match and `HttpClaimsDataProvider` to fail-open to an empty result. This is a
  *candidate* explanation for those two specific cases, not yet confirmed by live debugging.

### Also noticed (separate, latent regression — not yet causing failures, flagged for follow-up)

This branch's uncommitted diff to `ClaimValidationIntegrationTestBase` **removed** the
`@BeforeEach` cache-eviction of the library's `FeeSchemeProvider`/`ProviderDetailsProvider`
singleton HTTP caches:
```java
// present on main, REMOVED in this branch's working tree:
if (feeSchemeProvider instanceof AbstractHttpCachingProvider<?> cachingProvider) {
  cachingProvider.clear();
}
if (providerDetailsProvider instanceof AbstractHttpCachingProvider<?> cachingProvider) {
  cachingProvider.clear();
}
```
The original comment explained these providers cache responses in-memory (10 min positive / 10 sec
negative TTL) inside **singleton beans** shared across the whole Spring test context, and
`mockServerClient.reset()` does **not** clear these caches — only MockServer expectations. Checked
`ClaimValidation.fetchFeeCalculationType`: a fee-scheme failure adds a
`TECHNICAL_ERROR_FEE_SCHEME_API` issue but does **not** abort later validators, and no such issue
appears in any of the 9 failures, so this removal is **not** the direct cause of the current
failures — but it re-introduces a cross-test-order-dependent flakiness risk that should be restored
at some point (likely when addressing root cause #2, since it touches the same test base class).

## Agreed plan (per user 2026-09-11)

1. **Fix root cause #1 first** (mock stub bug in `ClaimValidationIntegrationTestBase`) — filter the
   per-claim duplicate-check stub response to only include claims that genuinely match on
   fee_code/UFN/UCN, mirroring real API filtering behaviour.
2. Re-run `DuplicateClaimValidatorITest` — expect the 5 false-positive ("existing=1" on
   `*-no-duplicate*` fixtures) cases to pass; the 4 genuine-duplicate cases will likely still fail
   with `new=0` (root cause #2, untouched).
3. **Then** revisit root cause #2 (library-side always-0) with live debugging, and decide whether to
   restore the `FeeSchemeProvider`/`ProviderDetailsProvider` cache-eviction `@BeforeEach` logic.

## Key file locations

- Production (this repo, changed this branch):
  `data-claims-event-service/src/main/java/.../validation/claim/duplicate/DuplicateClaimValidation.java`
  and sibling `DuplicateClaim*ValidationServiceStrategy.java` classes.
- Test harness (this repo, changed this branch):
  `data-claims-event-service/src/integrationTest/java/.../validator/claim/ClaimValidationIntegrationTestBase.java`
  `data-claims-event-service/src/integrationTest/java/.../helper/MockServerIntegrationTest.java`
  `data-claims-event-service/src/integrationTest/java/.../validator/claim/DuplicateClaimValidatorITest.java`
- Fixtures:
  `data-claims-event-service/src/integrationTest/resources/responses/data-claims/get-claims/DuplicateClaimValidator/*.json`
- External library sources (read-only reference, not in this repo):
  `~/.gradle/caches/modules-2/files-2.1/uk.gov.justice.laa.dstew.payments.claims.validation/claims-validation-core/1.4.10/*-sources.jar`
  Key classes: `validator/claim/rules/duplicate/DuplicateClaimValidation.java`,
  `validator/claim/rules/DuplicateClaimValidator.java`, `validator/claim/ClaimValidation.java`,
  `provider/impl/HttpClaimsDataProvider.java`, `config/ClaimsValidationAutoConfiguration.java`.

