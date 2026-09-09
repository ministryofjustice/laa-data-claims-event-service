package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DuplicateClaimValidatorITest extends ClaimValidationIntegrationTestBase {

  private static final String VALIDATOR_PATH = CLAIMS_BASE_PATH + "DuplicateClaimValidator/";
  private static final Set<String> DUPLICATE =
      Set.of("INVALID_CLAIM_HAS_DUPLICATE_IN_SAME_SUBMISSION");
  private static final Set<String> DUPLICATE_OTHER =
      Set.of("INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION");
  private static final Set<String> NONE = Set.of();

  // ── Previous-submission mock response fixtures ──────────────────────────
  private static final String PREV_CAPA_MATCH =
      CLAIMS_BASE_PATH + "DuplicateClaimValidator/previous-submission-capa-match.json";
  private static final String PREV_CL_MATCH =
      CLAIMS_BASE_PATH + "DuplicateClaimValidator/previous-submission-cl-match.json";
  private static final String PREV_NO_MATCH =
      CLAIMS_BASE_PATH + "DuplicateClaimValidator/previous-submission-no-match.json";

  static List<Arguments> claimsResponse() {
    return List.of(
        // ── Legal Help: same-submission ───────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-no-duplicate.json", NONE, null, null),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-duplicate.json", DUPLICATE, null, null),

        // ── Legal Help Disbursement (CAPA): same-submission ───────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-disbursement-no-duplicate.json", NONE, null, null),
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-disbursement-duplicate.json", DUPLICATE, null, null),

        // ── Legal Help Disbursement (CAPA): cross-submission ─────────────
        // Previous submission returns a matching CAPA claim concluded 2025-03-21 (1 day after
        // the 3-month cutoff of 2025-02-20 for APR-2025 submission) → duplicate
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-disbursement-cross-submission-duplicate.json",
            DUPLICATE_OTHER,
            "CAPA",
            PREV_CAPA_MATCH),
        // Previous submission returns no match → no error
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-disbursement-no-cross-submission-duplicate.json",
            NONE,
            "CAPA",
            PREV_NO_MATCH),

        // ── Crime Lower: same-submission ──────────────────────────────────
        Arguments.of(SUBMISSION_CRIME_LOWER, "cl-no-duplicate.json", NONE, null, null),
        Arguments.of(SUBMISSION_CRIME_LOWER, "cl-duplicate.json", DUPLICATE, null, null),

        // ── Crime Lower: cross-submission ─────────────────────────────────
        // TODO: test is invalid and needs review
        // Arguments.of(
        //    SUBMISSION_CRIME_LOWER,
        //    "cl-cross-submission-duplicate.json",
        //    DUPLICATE_OTHER,
        //    "CA",
        //    PREV_CL_MATCH),
        Arguments.of(
            SUBMISSION_CRIME_LOWER,
            "cl-no-cross-submission-duplicate.json",
            NONE,
            "CA",
            PREV_NO_MATCH),

        // ── Mediation: same-submission ────────────────────────────────────
        Arguments.of(SUBMISSION_MEDIATION, "med-no-duplicate.json", NONE, null, null),
        Arguments.of(SUBMISSION_MEDIATION, "med-duplicate.json", NONE, null, null));
    // no duplicate detection for mediation
  }

  @DisplayName("Duplicate claim validator - validate claim responses produce matching reports")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldMatchValidationReportForClaimsResponse(
      String submission,
      String claimsResponse,
      Set<String> ignored,
      String prevFeeCode,
      String prevMatchFixture)
      throws Exception {
    stubCrossSubmissionIfNeeded(submission, claimsResponse, prevFeeCode, prevMatchFixture);
    var context = runSubmissionValidationWithClaims(submission, VALIDATOR_PATH + claimsResponse);
    var claims = parseClaimsFromFixture(VALIDATOR_PATH + claimsResponse);
    for (var cr : claims) {
      assertExactMatchBetweenValidationAndReport(cr, claims, context);
    }
  }

  @DisplayName("Duplicate claim validator - should only produce expected error codes")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldOnlyProduceExpectedErrorCodes(
      String submission,
      String fixture,
      Set<String> expectedCodes,
      String prevFeeCode,
      String prevMatchFixture)
      throws Exception {
    stubCrossSubmissionIfNeeded(submission, fixture, prevFeeCode, prevMatchFixture);
    Set<String> actual = collectValidationIssueCodes(submission, VALIDATOR_PATH + fixture);
    assertEquals(expectedCodes, actual, "Unexpected or missing error codes for " + fixture);
  }

  /**
   * Stubs the GET /claims call that {@code getDuplicateClaimsInPreviousSubmission} makes when
   * checking for cross-submission duplicates. Only called for fixtures that exercise that path.
   */
  private void stubCrossSubmissionIfNeeded(
      String submissionFixture, String claimsFixture, String feeCode, String prevMatchFixture)
      throws Exception {
    if (feeCode == null || prevMatchFixture == null) {
      return;
    }
    // Read office code from submission fixture and UFN/UCN from first claim in claims fixture
    var submissionJson = mapper.readTree(readJsonFromFile(submissionFixture));
    String officeCode =
        submissionJson.has("office_account_number")
            ? submissionJson.get("office_account_number").asText()
            : "AQ2B3C";
    var claims = parseClaimsFromFixture(VALIDATOR_PATH + claimsFixture);
    if (claims.isEmpty()) return;
    var first = claims.getFirst();
    stubForGetClaimsFromPreviousSubmission(
        officeCode,
        feeCode,
        first.getUniqueFileNumber(),
        first.getUniqueClientNumber(),
        prevMatchFixture);
  }
}
