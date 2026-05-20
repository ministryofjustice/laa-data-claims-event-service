package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MandatoryFieldClaimValidatorITest extends ClaimValidationIntegrationTestBase {

  private static final String VALIDATOR_PATH = CLAIMS_BASE_PATH + "MandatoryFieldClaimValidator/";
  private static final Set<String> MISSING = Set.of("MISSING_MANDATORY_FIELD");
  private static final Set<String> NONE = Set.of();

  static List<Arguments> claimsResponse2() {
    return List.of(
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-disbursement-excluded-fields-valid.json", NONE));
  }

  static List<Arguments> claimsResponse() {
    return List.of(
        // ── Legal Help: valid ─────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-valid.json", NONE),

        // ── Legal Help: each mandatory field missing ──────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-unique-file-number.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-case-start-date.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-case-concluded-date.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-outcome-code.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-travel-waiting-costs-amount.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-client-forename.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-client-surname.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-client-date-of-birth.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-unique-client-number.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-client-postcode.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-gender-code.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-ethnicity-code.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-disability-code.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-advice-time.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-travel-time.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-waiting-time.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-net-counsel-costs-amount.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-case-id.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-case-reference-number.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-schedule-reference.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-matter-type-code.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-net-profit-costs-amount.json", MISSING),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-is-vat-applicable.json", MISSING),

        // ── Legal Help Disbursement: excluded fields null → no error ──────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-disbursement-excluded-fields-valid.json", NONE),

        // ── Legal Help non-disbursement: excluded field null → error ──────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-non-disbursement-excluded-field-missing.json", MISSING),

        // ── Crime Lower: valid ────────────────────────────────────────────
        Arguments.of(SUBMISSION_CRIME_LOWER, "cl-valid.json", NONE),

        // ── Crime Lower: each mandatory field missing ─────────────────────
        Arguments.of(SUBMISSION_CRIME_LOWER, "cl-missing-case-concluded-date.json", MISSING),
        Arguments.of(SUBMISSION_CRIME_LOWER, "cl-missing-stage-reached-code.json", MISSING),
        Arguments.of(SUBMISSION_CRIME_LOWER, "cl-missing-net-profit-costs-amount.json", MISSING),
        // TODO: this fails again due to 2 error messages and a miss config. VAT disbursement amount
        // is mandatory for crime lower, but the error message is currently configured to
        // be added by both the schema validation and the mandatory field validator, which
        // causes the collectValidationIssueCodes assertion to fail as it only expects one
        // error code per missing field. This should be resolved by removing the error code
        // from the schema validation error message for this field, as it's more appropriate
        // for it to be reported by the mandatory field validator.

        Arguments.of(SUBMISSION_CRIME_LOWER, "cl-missing-disbursements-vat-amount.json",
        MISSING),

        // ── Mediation: valid ──────────────────────────────────────────────
        Arguments.of(SUBMISSION_MEDIATION, "med-valid.json", NONE),

        // ── Mediation: each mandatory field missing ───────────────────────
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-outreach-location.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-referral-source.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-client-forename.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-client-surname.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-client-date-of-birth.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-unique-client-number.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-client-postcode.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-gender-code.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-ethnicity-code.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-disability-code.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-is-legally-aided.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-case-id.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-case-start-date.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-case-reference-number.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-schedule-reference.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-matter-type-code.json", MISSING),
        Arguments.of(SUBMISSION_MEDIATION, "med-missing-unique-case-id.json", MISSING));
  }

  @DisplayName("MandatoryField claim validator - validate claim responses produce matching reports")
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

  @DisplayName("MandatoryField claim validator - should only produce expected error codes")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldOnlyProduceExpectedErrorCodes(
      String submission, String fixture, Set<String> expectedCodes) throws Exception {
    Set<String> actual = collectValidationIssueCodes(submission, VALIDATOR_PATH + fixture);
    assertEquals(expectedCodes, actual, "Unexpected or missing error codes for " + fixture);
  }
}
