package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
            .caseStartDate("2025-08-14")
            .caseStartDate("2003-13-34")
            .transferDate("2090-12-02")
            .caseConcludedDate("2090-01-01")
            .representationOrderDate("2090-01-01")
            .matterTypeCode("a:b");

    SubmissionValidationContext context = new SubmissionValidationContext();

    validator.validate(claim, context);

    // Then
    assertThat(
        getClaimMessages(context, claimId.toString()).stream()
            .anyMatch(
                x ->
                    x.getDisplayMessage()
                        .equals(
                            "Invalid date value provided for Case Start Date: 2003-13-34")))
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
  void validatePastDatesTwo() {
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

    validator.validate(claim, context);

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


}