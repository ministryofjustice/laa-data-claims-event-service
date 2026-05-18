package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MatterTypeClaimValidatorITest extends ClaimValidationIntegrationTestBase {

  private static final String VALIDATOR_PATH = CLAIMS_BASE_PATH + "MatterTypeClaimValidator/";

  static List<Arguments> claimsResponse() {
    return List.of(
        Arguments.of(SUBMISSION_LEGAL_HELP, "legal-help-valid.json", Set.of()),
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "legal-help-invalid.json", Set.of("INVALID_MATTER_TYPE_CODE")),
        Arguments.of(SUBMISSION_MEDIATION, "mediation-valid.json", Set.of()),
        Arguments.of(
            SUBMISSION_MEDIATION, "mediation-invalid.json", Set.of("INVALID_MATTER_TYPE_CODE")),
        Arguments.of(SUBMISSION_CRIME_LOWER, "crime-lower-invalid.json", Set.of()));
  }

  @DisplayName("MatterType claim validator - validate claim responses produce matching reports")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldMatchValidationReportForClaimsResponse(
      String submission, String claimsResponse, Set<String> ignored) throws Exception {
    var context = runSubmissionValidationWithClaims(submission, VALIDATOR_PATH + claimsResponse);
    var claims = parseClaimsFromFixture(VALIDATOR_PATH + claimsResponse);
    for (var cr : claims) {
      assertExactMatchBetweenValidationAndReport(cr, context);
    }
  }

  @DisplayName("MatterType claim validator - should only produce expected error codes")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldOnlyProduceExpectedErrorCodes(
      String submission, String fixture, Set<String> expectedCodes) throws Exception {
    Set<String> actual = collectValidationIssueCodes(submission, VALIDATOR_PATH + fixture);
    assertEquals(expectedCodes, actual, "Unexpected or missing error codes for " + fixture);
  }
}
