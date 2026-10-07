package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration tests for {@link
 * uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.DisbursementClaimStartDateValidator}.
 *
 * <p>The validator checks that CAPA (disbursement) claims are submitted at least 3 calendar months
 * after the case start date. The cutoff for submission period APR-2025 is 20 MAY-2025, so:
 *
 * <ul>
 *   <li>{@code case_start_date + 3 months > 2025-05-20} → error
 *   <li>Boundary valid: {@code 2025-02-20 + 3m = 2025-05-20} — not after cutoff → no error
 *   <li>Boundary invalid: {@code 2025-02-21 + 3m = 2025-05-21 > 2025-05-20} → error
 * </ul>
 *
 * <p>The error is recorded by the old validator via {@code context.addClaimError} (not via the new
 * validation engine), so {@code shouldOnlyProduceExpectedErrorCodes} always expects {@code
 * Set.of()} — documenting that the new validator does not yet cover this rule.
 */
public class DisbursementClaimStartDateValidatorITest extends ClaimValidationIntegrationTestBase {

  private static final String VALIDATOR_PATH =
      CLAIMS_BASE_PATH + "DisbursementClaimStartDateValidator/";
  private static final Set<String> NONE = Set.of();

  static List<Arguments> claimsResponse() {
    return List.of(
        // ── Non-disbursement claim (fee_code=CA) — validator skips entirely ──
        Arguments.of(SUBMISSION_LEGAL_HELP, "non-disbursement-valid.json", NONE),

        // ── CAPA disbursement: case started well before 3-month window ──────
        // case_start_date=2024-01-01, +3m=2024-04-01, cutoff=2025-05-20 → valid
        Arguments.of(SUBMISSION_LEGAL_HELP, "disbursement-start-date-old-valid.json", NONE),

        // ── CAPA disbursement: case started exactly on the boundary ──────────
        // case_start_date=2025-02-20, +3m=2025-05-20, cutoff=2025-05-20 → valid (inclusive)
        Arguments.of(SUBMISSION_LEGAL_HELP, "disbursement-start-date-boundary-valid.json", NONE),

        // ── CAPA disbursement: case started 1 day after boundary ─────────────
        // case_start_date=2025-02-21, +3m=2025-05-21 > cutoff=2025-05-20 → error
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "disbursement-start-date-boundary-invalid.json",
            Set.of("DISBURSEMENT_TOO_EARLY")));
  }

  @DisplayName(
      "DisbursementClaimStartDate validator - validate claim responses produce matching reports")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldMatchValidationReportForClaimsResponse(
      String submission, String claimsResponse, Set<String> ignored) throws Exception {
    var context = runSubmissionValidationWithClaims(submission, VALIDATOR_PATH + claimsResponse);
    var claims = parseClaimsFromFixture(VALIDATOR_PATH + claimsResponse);
    for (var cr : claims) {
      assertExactMatchBetweenValidationAndReport(cr, claims, context);
    }
  }

  @DisplayName("DisbursementClaimStartDate validator - should only produce expected error codes")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldOnlyProduceExpectedErrorCodes(
      String submission, String fixture, Set<String> expectedCodes) throws Exception {
    Set<String> actual = collectValidationIssueCodes(submission, VALIDATOR_PATH + fixture);
    assertEquals(expectedCodes, actual, "Unexpected or missing error codes for " + fixture);
  }
}
