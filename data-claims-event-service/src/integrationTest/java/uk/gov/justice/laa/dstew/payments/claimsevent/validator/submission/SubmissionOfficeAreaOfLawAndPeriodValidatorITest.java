package uk.gov.justice.laa.dstew.payments.claimsevent.validator.submission;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

/**
 * Integration tests for {@link
 * uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission.SubmissionOfficeAreaOfLawAndPeriodValidator}.
 */
public class SubmissionOfficeAreaOfLawAndPeriodValidatorITest
    extends SubmissionValidationIntegrationTestBase {

  private static final String P =
      SUBMISSION_BASE_PATH + "SubmissionOfficeAreaOfLawAndPeriodValidator/";

  @Test
  @DisplayName("No prior VALIDATION_SUCCEEDED submission for same office/area/period - no error")
  void noDuplicateSubmissionIsValid() throws Exception {
    var ctx = runSubmissionValidation(P + "no-duplicate-submission.json", false);
    assertNoSubmissionErrors(ctx);
  }

  @Test
  @DisplayName(
      "Prior VALIDATION_SUCCEEDED submission exists for same office/area/period - should produce SUBMISSION_ALREADY_EXISTS")
  void duplicateSubmissionIsInvalid() throws Exception {
    var ctx = runSubmissionValidation(P + "duplicate-submission.json", true);
    assertSubmissionErrors(ctx, Set.of(SubmissionValidationError.SUBMISSION_ALREADY_EXISTS));
  }
}
