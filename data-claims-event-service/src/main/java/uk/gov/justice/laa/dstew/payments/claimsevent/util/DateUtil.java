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
public class DateUtil {

  private static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
  private static final String DATE_PATTERN_MMM_YYYY = "MMM-yyyy";
  private static final String DATE_FORMAT_DISPLAY_MESSAGE = "dd/MM/yyyy";

  public static final DateTimeFormatter DATE_FORMATTER_YYYY_MM_DD =
      DateTimeFormatter.ofPattern(DATE_FORMAT_YYYY_MM_DD);
  public static final DateTimeFormatter DATE_FORMATTER_FOR_DISPLAY_MESSAGE =
      DateTimeFormatter.ofPattern(DATE_FORMAT_DISPLAY_MESSAGE);

  /** Formatter for parsing submission period dates in the format "MMM-yyyy" (e.g. "JAN-2026"). */
  public static final DateTimeFormatter SUBMISSION_PERIOD_FORMATTER =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern(DATE_PATTERN_MMM_YYYY)
          .toFormatter(Locale.ENGLISH);

  /** Gets the current date as a {@link YearMonth} object. */
  public YearMonth currentYearMonth() {
    return YearMonth.now();
  }

  /**
   * Converts a submission period string into the last date of that month.
   *
   * @param submissionPeriod The submission period in format "MMM-yyyy" (e.g. "JAN-2026")
   * @param errorMessage The error message to display if the submissionPeriod string is blank
   * @return The last date of the specified month as a LocalDate
   * @throws DateTimeParseException if the submissionPeriod string cannot be parsed
   */
  public static LocalDate getLastDateOfMonth(String submissionPeriod, String errorMessage) {
    if (StringUtils.isBlank(submissionPeriod)) {
      throw new IllegalArgumentException(errorMessage);
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
   * @param oldestDateAllowed The oldest allowed date
   * @param newestDateAllowed The latest allowed date
   * @param context The validation context where any validation errors will be added
   * @param errorMessage The error message template to use when validation fails. Should contain two
   *     '%s' placeholders: first for the field name, second for the formatted oldest allowed date
   */
  public static void checkDateAllowed(
      ClaimResponse claim,
      String fieldName,
      String dateValueToCheck,
      LocalDate oldestDateAllowed,
      LocalDate newestDateAllowed,
      SubmissionValidationContext context,
      String errorMessage) {
    if (!StringUtils.isBlank(dateValueToCheck)) {
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
