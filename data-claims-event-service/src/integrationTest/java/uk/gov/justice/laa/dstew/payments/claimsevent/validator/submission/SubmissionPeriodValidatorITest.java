package uk.gov.justice.laa.dstew.payments.claimsevent.validator.submission;

import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

/**
 * Integration tests for {@link
 * uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission.SubmissionPeriodValidator}.
 *
 * <p>All tests use the pinned "current month" of MAY-2026 from the base class.
 */
public class SubmissionPeriodValidatorITest extends SubmissionValidationIntegrationTestBase {

  private static final String P = SUBMISSION_BASE_PATH + "SubmissionPeriodValidator/";

  @Test
  @DisplayName("APR-2025 (valid past period) - should produce no error")
  void validPastPeriodProducesNoError() throws Exception {
    var ctx = runSubmissionValidation(P + "period-valid-past.json");
    assertNoSubmissionErrors(ctx);
  }

  @Test
  @DisplayName("Null submission period - should produce SUBMISSION_PERIOD_MISSING")
  @Disabled(
      "Current service validator fails if period is null, so this should be "
          + "defended against in the new validator but can't be tested until then")
  void nullPeriodProducesMissingError() throws Exception {
    var ctx = runSubmissionValidation(P + "period-missing.json");
    assertSubmissionErrors(ctx, Set.of(SubmissionValidationError.SUBMISSION_PERIOD_MISSING));
  }

  @Test
  @DisplayName(
      "MAY-2026 (same as pinned current month) - should produce SUBMISSION_PERIOD_SAME_MONTH")
  void sameMonthPeriodIsInvalid() throws Exception {
    var ctx = runSubmissionValidation(P + "period-same-month.json");
    assertSubmissionErrors(ctx, Set.of(SubmissionValidationError.SUBMISSION_PERIOD_SAME_MONTH));
  }

  @Test
  @DisplayName("JUN-2026 (future month) - should produce SUBMISSION_PERIOD_FUTURE_MONTH")
  void futurePeriodIsInvalid() throws Exception {
    var ctx = runSubmissionValidation(P + "period-future.json");
    assertSubmissionErrors(ctx, Set.of(SubmissionValidationError.SUBMISSION_PERIOD_FUTURE_MONTH));
  }

  @Test
  @DisplayName(
      "MAR-2025 (before minimum APR-2025) - should produce SUBMISSION_VALIDATION_MINIMUM_PERIOD")
  void beforeMinimumPeriodIsInvalid() throws Exception {
    var ctx = runSubmissionValidation(P + "period-before-minimum.json");
    assertSubmissionErrors(
        ctx, Set.of(SubmissionValidationError.SUBMISSION_VALIDATION_MINIMUM_PERIOD));
  }
}
