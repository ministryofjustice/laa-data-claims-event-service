package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionAreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@DisplayName("Case dates claim validator test")
class CaseDatesClaimValidatorTest {

  private final CaseDatesClaimValidator validator = new CaseDatesClaimValidator();

  @Test
  void validatePastDatesOne() {
    UUID claimId = new UUID(1, 1);
    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .status(ClaimStatus.READY_TO_PROCESS)
            .feeCode("feeCode1")
            .caseStartDate("2003-13-34")
            .transferDate("2090-12-02")
            .caseConcludedDate("2090-01-01")
            .representationOrderDate("2090-01-01")
            .matterTypeCode("a:b");

    SubmissionValidationContext context = new SubmissionValidationContext();

    validator.validate(claim, context, BulkSubmissionAreaOfLaw.LEGAL_HELP);

    // Then
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals("Invalid date value provided for Case Start Date: 2003-13-34")))
        .isTrue();
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals(
                                "Invalid date value for Transfer Date (Must be between 1995-01-01 "
                                    + "and today): "
                                    + "2090-12-02")))
        .isTrue();
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals(
                                "Invalid date value for Case Concluded Date (Must be between "
                                    + "1995-01-01 and "
                                    + "today): 2090-01-01")))
        .isTrue();
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals(
                                "Invalid date value for Representation Order Date (Must be between "
                                    + "2016-04-01 "
                                    + "and today): 2090-01-01")))
        .isTrue();
  }

  @Test
  void validatePastDatesTwoLegalHelp() {
    UUID claimId = new UUID(2, 2);

    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .status(ClaimStatus.READY_TO_PROCESS)
            .feeCode("feeCode2")
            .caseStartDate("2025-05-25")
            .caseStartDate("1993-01-03")
            .transferDate("1990-12-02")
            .caseConcludedDate("1993-01-01")
            .representationOrderDate("2016-03-30")
            .matterTypeCode("1:2");

    SubmissionValidationContext context = new SubmissionValidationContext();

    validator.validate(claim, context, BulkSubmissionAreaOfLaw.LEGAL_HELP);

    // Then
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals(
                                "Invalid date value for Case Start Date (Must be between 1995-01-01"
                                    + " and today):"
                                    + " 1993-01-03")))
        .isTrue();
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals(
                                "Invalid date value for Transfer Date (Must be between 1995-01-01 "
                                    + "and today): "
                                    + "1990-12-02")))
        .isTrue();
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals(
                                "Invalid date value for Case Concluded Date (Must be between "
                                    + "1995-01-01 and "
                                    + "today): 1993-01-01")))
        .isTrue();
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals(
                                "Invalid date value for Representation Order Date (Must be between "
                                    + "2016-04-01 "
                                    + "and today): 2016-03-30")))
        .isTrue();
  }

  /*
  Case Concluded Date should be mandatory for LEGAL_HELP and CRIME_LOWER, but it's optional for MEDIATION.
  Ref: https://dsdmoj.atlassian.net/browse/DSTEW-566
   */
  @ParameterizedTest(
      name = "{index} => claimId={0}, areaOfLaw={1}, caseConcludedDate={2}, expectError={3}")
  @CsvSource({
    "1, LEGAL_HELP, 2025-08-14, false, null",
    "2, LEGAL_HELP, 1994-08-14, true, Invalid date value for Case Concluded Date (Must be between 1995-01-01 and today): 1994-08-14",
    "3, CRIME_LOWER, 2017-08-14, false, null",
    "4, CRIME_LOWER, 2015-08-14, true, Invalid date value for Case Concluded Date (Must be between 2016-04-01 and today): 2015-08-14",
    "5, CRIME_LOWER, 2099-08-14, true, Invalid date value for Case Concluded Date (Must be between 2016-04-01 and today): 2099-08-14",
    "6, MEDIATION, 1996-08-14, false, null",
    "7, MEDIATION, 1994-08-14, true, Invalid date value for Case Concluded Date (Must be between 1995-01-01 and today): 1994-08-14"
  })
  void checkMandatoryCaseConcludedDate(
      int claimIdBit,
      BulkSubmissionAreaOfLaw areaOfLaw,
      String caseConcludedDate,
      boolean expectError,
      String expectedErrorMsg) {
    UUID claimId = new UUID(claimIdBit, claimIdBit);
    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .caseStartDate("2025-08-14")
            .caseConcludedDate(caseConcludedDate);

    SubmissionValidationContext context = new SubmissionValidationContext();

    // Run validation
    validator.validate(claim, context, areaOfLaw);

    if (expectError) {
      assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
          .isEqualTo(expectedErrorMsg);
    } else {
      assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
    }
  }
}
