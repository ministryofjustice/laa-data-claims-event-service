package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MatterTypeClaimValidatorITest extends ClaimValidationIntegrationTestBase {

  static List<String> claimsResponse() {
    // Use dedicated fixtures for MatterType validator covering each area of law
    return List.of(
        "data-claims/get-claims/matter-type-claim-validator/legal-help-valid.json",
        "data-claims/get-claims/matter-type-claim-validator/legal-help-invalid.json",
        "data-claims/get-claims/matter-type-claim-validator/mediation-valid.json",
        "data-claims/get-claims/matter-type-claim-validator/mediation-invalid.json",
        "data-claims/get-claims/matter-type-claim-validator/crime-lower-invalid.json");
  }

  @DisplayName("MatterType claim validator - validate claim responses produce matching reports")
  @ParameterizedTest(name = "{0}")
  @MethodSource("claimsResponse")
  void shouldMatchValidationReportForClaimsResponse(String claimsResponse) throws Exception {
    // Run flow and compare

    // TODO: we need to set up submissions a legal help, crime lower and mediation

    var context = runSubmissionValidationWithClaims(claimsResponse);
    var claims = parseClaimsFromFixture(claimsResponse);
    for (var cr : claims) {
      // Each assertion will throw AssertionError on mismatch
      assertDoesNotThrow(() -> assertExactMatchBetweenValidationAndReport(cr, context));
    }
  }
}
