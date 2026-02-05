package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.checkDateAllowed;
import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.getLastDateOfMonth;

import java.time.LocalDate;
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
      String oldestDateAllowedStr,
      SubmissionValidationContext context) {

    checkDateAllowed(
        claim,
        fieldName,
        dateValueToCheck,
        oldestDateAllowedStr,
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
   * @param oldestDateAllowedStr The earliest allowed date in the format "yyyy-MM-dd"
   * @param context The validation context where any validation errors will be added
   */
  protected void checkDateInPastAndDoesNotExceedSubmissionPeriod(
      ClaimResponse claim,
      String fieldName,
      String dateValueToCheck,
      String oldestDateAllowedStr,
      SubmissionValidationContext context) {
    if (claim.getSubmissionPeriod() != null) {
      LocalDate lastDateOfMonth = getLastDateOfMonth(claim.getSubmissionPeriod());
      checkDateAllowed(
          claim,
          fieldName,
          dateValueToCheck,
          oldestDateAllowedStr,
          lastDateOfMonth,
          context,
          "%s cannot be later than the end date of the submission period or before %s");
    }
  }
}
