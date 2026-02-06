package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.DATE_FORMATTER_FOR_DISPLAY_MESSAGE;
import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.DATE_FORMATTER_YYYY_MM_DD;
import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.getLastDateOfMonth;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.util.StringUtils;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.ClaimValidator;

/**
 * Abstract class for validating dates.
 *
 * @author Jamie Briggs
 */
public abstract class AbstractDateValidator implements ClaimValidator {

  /**
   * Validates whether the provided date value is within the valid range (01/01/1995 to today's
   * date). If the date is invalid or falls outside the range, an error is added to the submission
   * validation context.
   *
   * @param claim The claim object associated with the date being checked.
   * @param fieldName The name of the field associated with the date being validated.
   * @param dateValueToCheck The date value to validate in the format "dd/MM/yyyy".
   */
  protected void checkDateInPast(
      ClaimResponse claim,
      String fieldName,
      String dateValueToCheck,
      LocalDate oldestDateAllowed,
      SubmissionValidationContext context) {

    checkDateAllowed(
        claim,
        fieldName,
        dateValueToCheck,
        oldestDateAllowed,
        LocalDate.now(),
        context,
        "%s must be between %s and today");
  }

  /**
   * Validates whether the provided date value is within the valid range and does not exceed the
   * submission period end date. The date must be between the oldest allowed date and the last day
   * of the submission period month. If the date is invalid or falls outside the range, an error is
   * added to the submission validation context.
   *
   * @param claim The claim object associated with the date being checked
   * @param fieldName The name of the field associated with the date being validated
   * @param dateValueToCheck The date value to validate in the format "dd/MM/yyyy"
   * @param oldestDateAllowed The earliest allowed date
   * @param context The validation context where any validation errors will be added
   */
  protected void checkDateInPastAndDoesNotExceedSubmissionPeriod(
      ClaimResponse claim,
      String fieldName,
      String dateValueToCheck,
      LocalDate oldestDateAllowed,
      SubmissionValidationContext context) {
    if (claim.getSubmissionPeriod() != null) {
      LocalDate lastDateOfMonth =
          getLastDateOfMonth(
              claim.getSubmissionPeriod(), "Submission period cannot be null or empty");
      checkDateAllowed(
          claim,
          fieldName,
          dateValueToCheck,
          oldestDateAllowed,
          lastDateOfMonth,
          context,
          "%s cannot be later than the end date of the submission period or before %s");
    }
  }

  /**
   * Validates whether a given date value falls within an allowed date range. If the date is invalid
   * or outside the specified range, an error is added to the validation context.
   *
   * @param claim The claim object associated with the date being validated
   * @param fieldName The name of the field being validated (used in error messages)
   * @param dateValueToCheck The date value to validate in the format "yyyy-MM-dd"
   * @param oldestDateAllowed The oldest allowed date
   * @param newestDateAllowed The latest allowed date
   * @param context The validation context where any validation errors will be added
   * @param errorMessage The error message template to use when validation fails. Should contain two
   *     '%s' placeholders: first for the field name, second for the formatted oldest allowed date
   */
  private static void checkDateAllowed(
      ClaimResponse claim,
      String fieldName,
      String dateValueToCheck,
      LocalDate oldestDateAllowed,
      LocalDate newestDateAllowed,
      SubmissionValidationContext context,
      String errorMessage) {
    if (StringUtils.hasText(dateValueToCheck)) {
      try {
        LocalDate date = LocalDate.parse(dateValueToCheck, DATE_FORMATTER_YYYY_MM_DD);

        if (date.isBefore(oldestDateAllowed) || date.isAfter(newestDateAllowed)) {
          context.addClaimError(
              claim.getId(),
              String.format(
                  errorMessage,
                  fieldName,
                  oldestDateAllowed.format(DATE_FORMATTER_FOR_DISPLAY_MESSAGE)),
              EVENT_SERVICE);
        }
      } catch (DateTimeParseException e) {
        context.addClaimError(
            claim.getId(),
            String.format("Invalid date value provided for %s", fieldName),
            EVENT_SERVICE);
      }
    }
  }
}
