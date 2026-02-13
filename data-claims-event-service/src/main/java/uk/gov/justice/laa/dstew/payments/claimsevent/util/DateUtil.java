package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import org.springframework.stereotype.Component;

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
}
