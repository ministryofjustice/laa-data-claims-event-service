package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
   * @param dateValueToCheck The date value to validate in the format "yyyy-MM-dd".
   */
  protected void checkDateInPast(
      ClaimResponse claim,
      String fieldName,
      String dateValueToCheck,
      String oldestDateAllowedStr,
      SubmissionValidationContext context) {
    if (!StringUtils.isEmpty(dateValueToCheck)) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      try {
        LocalDate oldestDateAllowed = LocalDate.parse(oldestDateAllowedStr, formatter);
        LocalDate date = LocalDate.parse(dateValueToCheck, formatter);
        if (date.isBefore(oldestDateAllowed) || date.isAfter(LocalDate.now())) {
          context.addClaimError(
              claim.getId(),
              String.format(
                  "Invalid date value for %s (Must be between %s and today): %s",
                  fieldName, oldestDateAllowedStr, dateValueToCheck),
              EVENT_SERVICE);
        }
      } catch (DateTimeParseException e) {
        context.addClaimError(
            claim.getId(),
            String.format("Invalid date value provided for %s: %s", fieldName, dateValueToCheck),
            EVENT_SERVICE);
      }
    }
  }
}
