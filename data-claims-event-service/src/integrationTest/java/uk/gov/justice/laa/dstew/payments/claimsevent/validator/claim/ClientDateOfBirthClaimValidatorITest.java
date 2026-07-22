package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ClientDateOfBirthClaimValidatorITest extends ClaimValidationIntegrationTestBase {

  private static final String VALIDATOR_PATH =
      CLAIMS_BASE_PATH + "ClientDateOfBirthClaimValidator/";

  static List<Arguments> claimsResponse() {
    return List.of(
        Arguments.of(SUBMISSION_LEGAL_HELP, "client-date-of-birth-valid.json", Set.of()),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "client-date-of-birth-too-old.json",
            Set.of("INVALID_CLIENT_DATE_OF_BIRTH")),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "client-date-of-birth-in-future.json",
            Set.of("INVALID_CLIENT_DATE_OF_BIRTH")),
        // TODO: invalid test as current does not dedupe and so has 2 errors when only should be 1
        //    Arguments.of(
        //    SUBMISSION_LEGAL_HELP,
        //    "client-date-of-birth-invalid-format.json",
        //    Set.of("INVALID_DATE_FORMAT")),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "client-2-date-of-birth-too-old.json",
            Set.of("INVALID_CLIENT_2_DATE_OF_BIRTH")),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "client-2-date-of-birth-in-future.json",
            Set.of("INVALID_CLIENT_2_DATE_OF_BIRTH")));
    // TODO: invalid test as current does not dedupe and so has 2 errors when only should be 1
    //    Arguments.of(
    //    SUBMISSION_LEGAL_HELP,
    //    "client-2-date-of-birth-invalid-format.json",
    //    Set.of("INVALID_DATE_FORMAT")));
  }

  @DisplayName(
      "ClientDateOfBirth claim validator - validate claim responses produce matching reports")
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

  @DisplayName("ClientDateOfBirth claim validator - should only produce expected error codes")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldOnlyProduceExpectedErrorCodes(
      String submission, String fixture, Set<String> expectedCodes) throws Exception {
    Set<String> actual = collectValidationIssueCodes(submission, VALIDATOR_PATH + fixture);
    assertEquals(expectedCodes, actual, "Unexpected or missing error codes for " + fixture);
  }
}
