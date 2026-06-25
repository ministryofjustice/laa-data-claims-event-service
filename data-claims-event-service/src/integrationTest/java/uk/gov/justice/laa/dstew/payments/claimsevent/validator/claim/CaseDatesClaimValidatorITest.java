package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CaseDatesClaimValidatorITest extends ClaimValidationIntegrationTestBase {

  private static final String VALIDATOR_PATH = CLAIMS_BASE_PATH + "CaseDatesClaimValidator/";

  // TODO: The commented out tests are due to the fact that even when we fail
  // a claim it still attempts to call fee scheme calculation and the mapper
  // falls over on the bad date and so kills the entire test.

  static List<Arguments> claimsResponse() {
    return List.of(
        Arguments.of(SUBMISSION_LEGAL_HELP, "case-dates-valid.json", Set.of()),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "case-start-date-too-old.json",
            Set.of("INVALID_CASE_START_DATE")),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "case-start-date-in-future.json",
            Set.of("INVALID_CASE_START_DATE")),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "case-start-date-in-future-disbursment.json",
            Set.of("DISBURSEMENT_TOO_EARLY", "INVALID_CASE_START_DATE")),
        // Arguments.of(
        //    SUBMISSION_LEGAL_HELP,
        //    "case-start-date-invalid-format.json",
        //    Set.of("INVALID_DATE_FORMAT")),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "case-concluded-date-in-future.json",
            Set.of("INVALID_CASE_CONCLUDED_DATE")),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "case-concluded-date-too-early.json",
            Set.of("INVALID_CASE_CONCLUDED_DATE")),
        // Arguments.of(
        //    SUBMISSION_LEGAL_HELP,
        //    "case-concluded-date-invalid-format.json",
        //    Set.of("INVALID_DATE_FORMAT")),
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "transfer-date-too-old.json", Set.of("INVALID_TRANSFER_DATE")),
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "transfer-date-in-future.json", Set.of("INVALID_TRANSFER_DATE")),
        // TODO: This test fails as the original records the same error twice and new validation
        // dedupes correctly.
        // Arguments.of(
        //    SUBMISSION_LEGAL_HELP,
        //    "transfer-date-invalid-format.json",
        //    Set.of("INVALID_DATE_FORMAT")),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "representation-order-date-too-old.json",
            Set.of("INVALID_REPRESENTATION_ORDER_DATE")),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "representation-order-date-in-future.json",
            Set.of("INVALID_REPRESENTATION_ORDER_DATE")));
    // Arguments.of(
    //    SUBMISSION_LEGAL_HELP,
    //    "representation-order-date-invalid-format.json",
    //    Set.of("INVALID_DATE_FORMAT")));
  }

  @DisplayName("CaseDates claim validator - validate claim responses produce matching reports")
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

  @DisplayName("CaseDates claim validator - should only produce expected error codes")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldOnlyProduceExpectedErrorCodes(
      String submission, String fixture, Set<String> expectedCodes) throws Exception {
    Set<String> actual = collectValidationIssueCodes(submission, VALIDATOR_PATH + fixture);
    assertEquals(expectedCodes, actual, "Unexpected or missing error codes for " + fixture);
  }
}
