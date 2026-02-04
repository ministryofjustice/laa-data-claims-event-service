package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
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
   * Validates whether the provided date value is within the valid range (from oldest allowed date
   * to the end of submission period month). If the date is invalid or falls outside the range, an
   * error is added to the submission validation context.
   *
   * @param claim The claim object containing submission period and associated data
   * @param fieldName The name of the field associated with the date being validated
   * @param dateValueToCheck The date value to validate in the format "yyyy-MM-dd"
   * @param oldestDateAllowedStr The oldest allowed date in the format "yyyy-MM-dd"
   * @param context The validation context where any validation errors will be added
   */
  protected void checkDateInPastAndDoesNotExceedSubmissionPeriod(
      ClaimResponse claim,
      String fieldName,
      String dateValueToCheck,
      String oldestDateAllowedStr,
      SubmissionValidationContext context) {
    if (claim.getSubmissionPeriod() != null) {
      DateTimeFormatter submissionPeriodFormatter =
          new DateTimeFormatterBuilder()
              .parseCaseInsensitive()
              .appendPattern("MMM-yyyy")
              .toFormatter(Locale.ENGLISH);
      YearMonth yearMonth = YearMonth.parse(claim.getSubmissionPeriod(), submissionPeriodFormatter);

      LocalDate lastDateOfMonth = yearMonth.atEndOfMonth();
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

  private static void checkDateAllowed(
      ClaimResponse claim,
      String fieldName,
      String dateValueToCheck,
      String oldestDateAllowedStr,
      LocalDate newestDateAllowed,
      SubmissionValidationContext context,
      String errorMessage) {
    if (!StringUtils.isEmpty(dateValueToCheck)) {
      try {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatterForMessage = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        LocalDate oldestDateAllowed = LocalDate.parse(oldestDateAllowedStr, formatter);
        LocalDate date = LocalDate.parse(dateValueToCheck, formatter);

        if (date.isBefore(oldestDateAllowed) || date.isAfter(newestDateAllowed)) {
          context.addClaimError(
              claim.getId(),
              String.format(errorMessage, fieldName, oldestDateAllowed.format(formatterForMessage)),
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
