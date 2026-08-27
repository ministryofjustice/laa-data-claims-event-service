package uk.gov.justice.laa.dstew.payments.claimsevent.validator.submission;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

/**
 * Integration tests for {@link
 * uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission.SubmissionStatusValidator}.
 */
public class SubmissionStatusValidatorITest extends SubmissionValidationIntegrationTestBase {

  private static final String P = SUBMISSION_BASE_PATH + "SubmissionStatusValidator/";

  @Test
  @DisplayName("Status READY_FOR_VALIDATION - should produce no error")
  void readyForValidationIsValid() throws Exception {
    var ctx = runSubmissionValidation(P + "status-ready-for-validation.json");
    assertNoSubmissionErrors(ctx);
  }

  @Test
  @DisplayName("Status VALIDATION_IN_PROGRESS - should produce no error")
  void validationInProgressIsValid() throws Exception {
    var ctx = runSubmissionValidation(P + "status-validation-in-progress.json");
    assertNoSubmissionErrors(ctx);
  }

  @Test
  @DisplayName(
      "Status VALIDATION_SUCCEEDED - should produce INCORRECT_SUBMISSION_STATUS_FOR_VALIDATION")
  void incorrectStatusIsInvalid() throws Exception {
    var ctx = runSubmissionValidation(P + "status-incorrect.json");
    assertSubmissionErrors(
        ctx, Set.of(SubmissionValidationError.INCORRECT_SUBMISSION_STATUS_FOR_VALIDATION));
  }
}
