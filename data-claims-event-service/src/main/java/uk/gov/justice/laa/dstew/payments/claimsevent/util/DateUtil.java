package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Utility class for handling date-related operations in the claims event service. Provides
 * functionality for:
 *
 * <ul>
 *   <li>Date formatting and parsing
 *   <li>Date validation within specified ranges
 *   <li>Submission period calculations
 * </ul>
 *
 * <p>The class supports standard date formats and implements validation logic for claim submission
 * dates.
 */
@Component
public final class DateUtil {

  private static final String DATE_FORMAT = "yyyy-MM-dd";
  private static final String DATE_FORMAT_MESSAGE = "dd/MM/yyyy";

  private DateUtil() {
    // Private constructor to prevent instantiation
  }

  /** Formatter for parsing submission period dates in the format "MMM-yyyy" (e.g. "JAN-2026"). */
  public static final DateTimeFormatter SUBMISSION_PERIOD_FORMATTER =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("MMM-yyyy")
          .toFormatter(Locale.ENGLISH);

  /** Gets the current date as a {@link YearMonth} object. */
  public YearMonth currentYearMonth() {
    return YearMonth.now();
  }

  /**
   * Converts a submission period string into the last date of that month.
   *
   * @param submissionPeriod The submission period in format "MMM-yyyy" (e.g. "JAN-2026")
   * @return The last date of the specified month as a LocalDate
   * @throws DateTimeParseException if the submissionPeriod string cannot be parsed
   */
  public static LocalDate getLastDateOfMonth(String submissionPeriod) {
    if (StringUtils.isBlank(submissionPeriod)) {
      throw new IllegalArgumentException("Submission period cannot be null or empty");
    }
    YearMonth yearMonth = YearMonth.parse(submissionPeriod, SUBMISSION_PERIOD_FORMATTER);
    return yearMonth.atEndOfMonth();
  }

  /**
   * Validates whether a given date value falls within an allowed date range. If the date is invalid
   * or outside the specified range, an error is added to the validation context.
   *
   * @param claim The claim object associated with the date being validated
   * @param fieldName The name of the field being validated (used in error messages)
   * @param dateValueToCheck The date value to validate in the format "yyyy-MM-dd"
   * @param oldestDateAllowedStr The earliest allowed date in the format "yyyy-MM-dd"
   * @param newestDateAllowed The latest allowed date
   * @param context The validation context where any validation errors will be added
   * @param errorMessage The error message template to use when validation fails. Should contain two
   *     '%s' placeholders: first for the field name, second for the formatted oldest allowed date
   */
  public static void checkDateAllowed(
      ClaimResponse claim,
      String fieldName,
      String dateValueToCheck,
      String oldestDateAllowedStr,
      LocalDate newestDateAllowed,
      SubmissionValidationContext context,
      String errorMessage) {
    if (!StringUtils.isEmpty(dateValueToCheck)) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
      DateTimeFormatter formatterForMessage = DateTimeFormatter.ofPattern(DATE_FORMAT_MESSAGE);
      try {
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
