package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@DisplayName("Client date of birth claim validator test")
class ClientDateOfBirthClaimValidatorTest {

  private final ClientDateOfBirthClaimValidator validator = new ClientDateOfBirthClaimValidator();

  @Test
  void validateClientDateOfBirthOne() {
    UUID claimId = new UUID(1, 1);
    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .status(ClaimStatus.READY_TO_PROCESS)
            .feeCode("feeCode1")
            .clientDateOfBirth("2099-12-31")
            .client2DateOfBirth("2099-12-31")
            .matterTypeCode("a:b");

    SubmissionValidationContext context = new SubmissionValidationContext();

    validator.validate(claim, context);

    // Then
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals("Client Date of Birth must be between 01/01/1900 and today")))
        .isTrue();
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals("Client Date of Birth must be between 01/01/1900 and today")))
        .isTrue();
  }

  @Test
  void validateClientDateOfBirthTwo() {
    UUID claimId = new UUID(2, 2);

    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .status(ClaimStatus.READY_TO_PROCESS)
            .feeCode("feeCode2")
            .clientDateOfBirth("1899-12-31")
            .client2DateOfBirth("1899-12-31")
            .matterTypeCode("1:2");

    SubmissionValidationContext context = new SubmissionValidationContext();

    validator.validate(claim, context);

    // Then
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals("Client Date of Birth must be between 01/01/1900 and today")))
        .isTrue();
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals("Client Date of Birth must be between 01/01/1900 and today")))
        .isTrue();
    assertThat(
            getClaimMessages(context, claimId.toString()).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals("Client 2 Date of Birth must be between 01/01/1900 and today")))
        .isTrue();
  }
}
