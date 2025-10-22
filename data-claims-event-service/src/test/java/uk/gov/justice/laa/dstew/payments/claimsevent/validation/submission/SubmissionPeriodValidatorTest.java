package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

@ExtendWith(MockitoExtension.class)
@DisplayName("Submission Period Validator Test")
class SubmissionPeriodValidatorTest {

  private static final String SUBMISSION_VALIDATION_MINIMUM_PERIOD = "APR-2025";

  @Mock DateUtil dateUtil;

  SubmissionPeriodValidator validator;

  @BeforeEach
  void beforeEach() {
    // Create validator
    validator = new SubmissionPeriodValidator(dateUtil, SUBMISSION_VALIDATION_MINIMUM_PERIOD);
    // Fixed date for testing
    YearMonth fixedMonth = YearMonth.of(2025, 5);

    lenient().when(dateUtil.currentYearMonth()).thenReturn(fixedMonth);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Should have error if submission period is null or empty")
  void shouldHaveErrorIfSubmissionPeriodIsEmpty(String submissionPeriod) {
    // Given
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().submissionPeriod(submissionPeriod).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext, SubmissionValidationError.SUBMISSION_PERIOD_MISSING);
  }

  @ParameterizedTest
  @ValueSource(strings = {"2025-05", "2025-05-01", "2025-05-01T12:00:00"})
  @DisplayName("Should have error if submission period invalid format")
  void shouldHaveErrorIfSubmissionPeriodInvalidFormat(String submissionPeriod) {
    // Given
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().submissionPeriod(submissionPeriod).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext, SubmissionValidationError.SUBMISSION_PERIOD_INVALID_FORMAT);
  }

  @Test
  @DisplayName("Should have no errors when claim month before this month")
  void shouldHaveNoErrorsWhenClaimMonthBeforeThisMonth() {
    // Given
    String submissionPeriod = SUBMISSION_VALIDATION_MINIMUM_PERIOD;
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().submissionPeriod(submissionPeriod).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertFalse(submissionValidationContext.hasErrors());
  }

  @Test
  @DisplayName("Should have errors when claim month is before this month")
  void shouldHaveErrorsWhenClaimMonthIsBeforeThisMonth() {
    // Given
    String submissionPeriod = "MAR-2025";
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().submissionPeriod(submissionPeriod).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext,
        SubmissionValidationError.SUBMISSION_VALIDATION_MINIMUM_PERIOD,
        SUBMISSION_VALIDATION_MINIMUM_PERIOD,
        SUBMISSION_VALIDATION_MINIMUM_PERIOD);
  }

  @Test
  @DisplayName("Should have error when claim month same as current month")
  void shouldHaveErrorWhenClaimMonthSameAsCurrentMonth() {
    // Given
    String submissionPeriod = "May-2025";
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().submissionPeriod(submissionPeriod).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext,
        SubmissionValidationError.SUBMISSION_PERIOD_SAME_MONTH,
        "May 2025");
  }

  @Test
  @DisplayName("Should have error when claim month after this month")
  void shouldHaveErrorWhenClaimMonthAfterCurrentMonth() {
    // Given
    String submissionPeriod = "AUG-2025";
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().submissionPeriod(submissionPeriod).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext,
        SubmissionValidationError.SUBMISSION_PERIOD_FUTURE_MONTH,
        "May 2025");
  }
}
