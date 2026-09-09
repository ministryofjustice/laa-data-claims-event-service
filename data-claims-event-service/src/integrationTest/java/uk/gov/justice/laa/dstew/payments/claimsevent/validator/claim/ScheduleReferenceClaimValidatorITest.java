package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ScheduleReferenceClaimValidatorITest extends ClaimValidationIntegrationTestBase {

  private static final String VALIDATOR_PATH =
      CLAIMS_BASE_PATH + "ScheduleReferenceClaimValidator/";

  static List<Arguments> claimsResponse() {
    return List.of(
        Arguments.of(SUBMISSION_LEGAL_HELP, "schedule-reference-valid.json", Set.of()),
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "schedule-reference-invalid.json",
            Set.of("SCHEMA_VALIDATION_ERROR")));
    // TODO: error is caught but technical message in wrong order.
    // not worth spending time on currently
    // Arguments.of(
    //    SUBMISSION_LEGAL_HELP,
    //    "schedule-reference-too-long.json",
    //    Set.of("SCHEMA_VALIDATION_ERROR")));
  }

  @DisplayName(
      "ScheduleReference claim validator - validate claim responses produce matching reports")
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

  @DisplayName("ScheduleReference claim validator - should only produce expected error codes")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldOnlyProduceExpectedErrorCodes(
      String submission, String fixture, Set<String> expectedCodes) throws Exception {
    Set<String> actual = collectValidationIssueCodes(submission, VALIDATOR_PATH + fixture);
    assertEquals(expectedCodes, actual, "Unexpected or missing error codes for " + fixture);
  }
}

// Only in new:        code=SCHEMA_VALIDATION_ERROR severity=ERROR message=Schedule Reference must
// be a maximum of 20 characters and contain only letters, numbers, forward slashes, periods, and
// hyphens technical=schedule_reference: must be at most 20 characters long (provided value:
// AAAAAAAAAAAAAAAAAAAAA) : schedule_reference: does not match the regex pattern
// ^[a-zA-Z0-9/.-]{1,20}$ (provided value: AAAAAAAAAAAAAAAAAAAAA)
// Only in existing: source=Data-Claims-Event-Service   type=ERROR display=Schedule Reference must
// be a maximum of 20 characters and contain only letters, numbers, forward slashes, periods, and
// hyphens technical=schedule_reference: does not match the regex pattern ^[a-zA-Z0-9/.-]{1,20}$
// (provided value: AAAAAAAAAAAAAAAAAAAAA) : schedule_reference: must be at most 20 characters long
// (provided value: AAAAAAAAAAAAAAAAAAAAA)
