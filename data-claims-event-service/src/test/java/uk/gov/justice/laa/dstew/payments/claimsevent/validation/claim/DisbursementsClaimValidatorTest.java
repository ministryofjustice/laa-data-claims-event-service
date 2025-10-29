package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionAreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@DisplayName("Disbursements claim validator test")
class DisbursementsClaimValidatorTest {

  DisbursementsClaimValidator validator = new DisbursementsClaimValidator();

  @ParameterizedTest(
      name =
          "{index} => claimId={0}, disbursementVatAmount={1}, areaOfLaw={2}, maxAllowed={3}, "
              + "expectError={4}")
  @CsvSource({
    "1, 99999.99, LEGAL_HELP, 99999.99, false",
    "2, 999999.99, CRIME_LOWER, 999999.99, false",
    "3, 999999999.99, MEDIATION, 999999999.99, false",
    "4, 100000.0, LEGAL_HELP, 99999.99, true",
    "5, 1000000.0, CRIME_LOWER, 999999.99, true",
    "6, 1000000000.0, MEDIATION, 999999999.99, true",
  })
  void checkDisbursementsVatAmount(
      int claimIdBit,
      BigDecimal disbursementsVatAmount,
      BulkSubmissionAreaOfLaw areaOfLaw,
      BigDecimal maxAllowed,
      boolean expectError) {
    UUID claimId = new UUID(claimIdBit, claimIdBit);
    ClaimResponse claim =
        new ClaimResponse().id(claimId.toString()).disbursementsVatAmount(disbursementsVatAmount);
    if (BulkSubmissionAreaOfLaw.CRIME_LOWER.equals(areaOfLaw)) {
      claim.setStageReachedCode("ABCD");
    }

    SubmissionValidationContext context = new SubmissionValidationContext();

    // Run validation
    validator.validate(claim, context, areaOfLaw);

    if (expectError) {
      String expectedMessage =
          String.format(
              "disbursementsVatAmount (%s): must have a maximum value of %s (provided value: %s)",
              areaOfLaw, maxAllowed, disbursementsVatAmount);
      assertThat(getClaimMessages(context, claimId.toString()).getFirst().getDisplayMessage())
          .isEqualTo(expectedMessage);
    } else {
      for (var claimReport : context.getClaimReports()) {
        assertThat(claimReport.hasErrors()).isFalse();
      }
    }
  }
}
