package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;

/**
 * Utility class to support date-related operations in the claims event service. Provides
 * functionality for date formatting and parsing.
 *
 * <p>The class supports standard date formats used across the service.
 */
@Component
public class DateUtil {

  private static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
  private static final String DATE_FORMAT_MMM_YYYY = "MMM-yyyy";
  private static final String DATE_FORMAT_DISPLAY_MESSAGE = "dd/MM/yyyy";

  public static final DateTimeFormatter DATE_FORMATTER_YYYY_MM_DD =
      DateTimeFormatter.ofPattern(DATE_FORMAT_YYYY_MM_DD);
  public static final DateTimeFormatter DATE_FORMATTER_FOR_DISPLAY_MESSAGE =
      DateTimeFormatter.ofPattern(DATE_FORMAT_DISPLAY_MESSAGE);

  /** Formatter for parsing submission period dates in the format "MMM-yyyy" (e.g. "JAN-2026"). */
  public static final DateTimeFormatter SUBMISSION_PERIOD_FORMATTER =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern(DATE_FORMAT_MMM_YYYY)
          .toFormatter(Locale.ENGLISH);

  /** Gets the current date as a {@link YearMonth} object. */
  public YearMonth currentYearMonth() {
    return YearMonth.now();
  }

  /**
   * Parses the given date string using the standard {@code yyyy-MM-dd} format.
   *
   * @param dateStr the date string to parse
   * @param fieldName a human-readable name for the field, used in the exception message
   * @return the parsed {@link LocalDate}
   * @throws EventServiceIllegalArgumentException if the string cannot be parsed
   */
  public static LocalDate parseDate(final String dateStr, final String fieldName) {
    try {
      return LocalDate.parse(dateStr, DATE_FORMATTER_YYYY_MM_DD);
    } catch (DateTimeParseException e) {
      throw new EventServiceIllegalArgumentException(
          String.format("Invalid date format for %s: %s", fieldName, dateStr));
    }
  }
}
