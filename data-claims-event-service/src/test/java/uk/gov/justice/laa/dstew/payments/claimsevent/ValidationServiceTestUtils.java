package uk.gov.justice.laa.dstew.payments.claimsevent;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import org.assertj.core.api.InstanceOfAssertFactories;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

public class ValidationServiceTestUtils {

  public static void assertContextClaimError(
      SubmissionValidationContext context,
      String claimId,
      ClaimValidationError claimValidationError) {
    ValidationMessagePatch messagePatch = claimValidationError.toPatch();
    assertThat(context.getClaimReport(claimId))
        .isPresent()
        .get()
        .extracting(ClaimValidationReport::getMessages)
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .containsExactly(messagePatch);
  }

  public static void assertContextClaimError(
      SubmissionValidationContext context, ClaimValidationError claimValidationError) {
    assertThat(context.getSubmissionValidationErrors())
        .isNotEmpty()
        .contains(claimValidationError.toPatch());
  }

  public static void assertContextClaimError(
      SubmissionValidationContext context,
      SubmissionValidationError submissionValidationError,
      Object... args) {
    assertThat(context.getSubmissionValidationErrors())
        .isNotEmpty()
        .contains(submissionValidationError.toPatch(args));
  }

  public static void assertContextClaimError(SubmissionValidationContext context, String message) {
    ValidationMessagePatch messagePatch = new ValidationMessagePatch();
    messagePatch
        .displayMessage(message)
        .technicalMessage(message)
        .source(EVENT_SERVICE)
        .type(ValidationMessageType.ERROR);
    assertThat(context.getSubmissionValidationErrors()).isNotEmpty().contains(messagePatch);
  }
}
