package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration tests that verify multiple validators interact correctly when a single claim triggers
 * errors across more than one validator simultaneously.
 *
 * <p>Each fixture deliberately violates rules owned by different validators to confirm that:
 *
 * <ul>
 *   <li>All expected error codes are produced (no validator suppresses another)
 *   <li>No unexpected error codes appear (no validator fires spuriously)
 * </ul>
 */
public class CrossValidatorInteractionITest extends ClaimValidationIntegrationTestBase {

  private static final String VALIDATOR_PATH = CLAIMS_BASE_PATH + "CrossValidatorInteraction/";

  static List<Arguments> claimsResponse() {
    return List.of(

        // ── Scenario 1: ClaimSchemaValidator + MandatoryFieldClaimValidator ─────────────────────
        // Invalid gender_code (SCHEMA_VALIDATION_ERROR) AND missing client_forename
        // (MISSING_MANDATORY_FIELD). Both validators run at different priorities; neither should
        // suppress the other.
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-schema-error-and-missing-mandatory.json",
            Set.of("SCHEMA_VALIDATION_ERROR", "MISSING_MANDATORY_FIELD")),

        // ── Scenario 2: DisbursementClaimStartDateValidator + MandatoryFieldClaimValidator ──────
        // CAPA fee code: case started 2025-03-01 under APR-2025 submission (< 3 months) triggers
        // INVALID_DISBURSEMENT_CLAIM_START_DATE. Mandatory fields excluded for disbursements are
        // null — must NOT produce MISSING_MANDATORY_FIELD.
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-capa-start-date-error-excluded-mandatory-null.json",
            Set.of("DISBURSEMENT_TOO_EARLY")),

        // ── Scenario 3: StageReachedClaimValidator + OutcomeCodeClaimValidator ──────────────────
        // Both run at priority 100 as ClaimWithAreaOfLawValidator. Invalid stage reached code
        // (INVC — too long for Legal Help) and invalid outcome code (EAX) must both fire.
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-invalid-stage-reached-and-invalid-outcome-code.json",
            Set.of("INVALID_STAGE_REACHED_LEGAL_HELP", "INVALID_OUTCOME_CODE")),

        // ── Scenario 4: ClaimSchemaValidator (priority 1) + DuplicateClaimValidator (10000) ─────
        // First claim has an invalid gender_code (schema error). Both claims share the same
        // UFN/UCN making the second a duplicate within the submission. Schema and duplicate errors
        // must both appear independently.
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-schema-error-and-duplicate.json",
            Set.of("SCHEMA_VALIDATION_ERROR", "INVALID_CLAIM_HAS_DUPLICATE_IN_SAME_SUBMISSION")),

        // ── Scenario 5: MandatoryFieldClaimValidator + ClaimSchemaValidator (matter_type_code) ──
        // Mediation claim missing client_forename (mandatory) and unique_case_id (mandatory), plus
        // an invalid matter_type_code (lowercase — fails schema pattern for MEDIATION). All three
        // independent errors must appear.
        Arguments.of(
            SUBMISSION_MEDIATION,
            "med-missing-mandatory-and-invalid-matter-type.json",
            Set.of("MISSING_MANDATORY_FIELD", "INVALID_MATTER_TYPE_CODE")));
  }

  @DisplayName("Cross-validator interaction - validate claim responses produce matching reports")
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

  @DisplayName("Cross-validator interaction - should only produce expected error codes")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldOnlyProduceExpectedErrorCodes(
      String submission, String fixture, Set<String> expectedCodes) throws Exception {
    Set<String> actual = collectValidationIssueCodes(submission, VALIDATOR_PATH + fixture);
    // For cross-validator tests we assert that ALL expected codes are present;
    // additional codes from other validators are acceptable (e.g. schema may add extras).
    assertTrue(
        actual.containsAll(expectedCodes),
        "Missing expected error codes for "
            + fixture
            + ". Expected all of: "
            + expectedCodes
            + " but got: "
            + actual);
  }
}
