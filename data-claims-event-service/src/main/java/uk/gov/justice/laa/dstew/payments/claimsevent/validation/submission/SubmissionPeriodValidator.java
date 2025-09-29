package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

/**
 * Validates that a submission's period is valid. Submission period should be in the format
 * MMM-yyyy.
 *
 * @author Jamie Briggs
 */
@Component
public class SubmissionPeriodValidator implements SubmissionValidator {

  private final DateUtil dateUtil;

  private DateTimeFormatter formatter;

  /**
   * Creates a new {@code SubmissionPeriodValidator}. Takes in a {@link DateUtil} instance to get
   * the current month.
   *
   * @param dateUtil the {@code DateUtil} instance to use to get the current month.
   */
  public SubmissionPeriodValidator(DateUtil dateUtil) {
    this.dateUtil = dateUtil;
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
    formatter =
        builder.parseCaseInsensitive().appendPattern("MMM-yyyy").toFormatter(Locale.ENGLISH);
  }

  /**
   * Validates that a submission's period is valid. Submission period should be in the format
   * MMM-yyyy.
   *
   * @param submission the submission to validate
   * @param context the validation context to add errors to
   */
  @Override
  public void validate(SubmissionResponse submission, SubmissionValidationContext context) {

    if (StringUtils.isEmpty(submission.getSubmissionPeriod())) {
      context.addSubmissionValidationError(SubmissionValidationError.SUBMISSION_PERIOD_MISSING);
      return;
    }

    try {
      YearMonth enteredSubmissionPeriod =
          YearMonth.parse(submission.getSubmissionPeriod(), formatter);
      YearMonth currentMonth = dateUtil.currentYearMonth();

      if (Objects.equals(enteredSubmissionPeriod, currentMonth)) {
        context.addSubmissionValidationError(
            SubmissionValidationError.SUBMISSION_PERIOD_SAME_MONTH, getReadableCurrentMonth());
      } else if (enteredSubmissionPeriod.isAfter(currentMonth)) {
        context.addSubmissionValidationError(
            SubmissionValidationError.SUBMISSION_PERIOD_FUTURE_MONTH, getReadableCurrentMonth());
      }
    } catch (DateTimeParseException e) {
      // Add error if date format is incorrect
      context.addSubmissionValidationError(
          SubmissionValidationError.SUBMISSION_PERIOD_INVALID_FORMAT);
    }
  }

  private String getReadableCurrentMonth() {
    return DateTimeFormatter.ofPattern("MMMM yyyy").format(dateUtil.currentYearMonth());
  }

  /**
   * Priority of this validator (lower has higher priority).
   *
   * @return the priority
   */
  @Override
  public int priority() {
    return 10;
  }
}
