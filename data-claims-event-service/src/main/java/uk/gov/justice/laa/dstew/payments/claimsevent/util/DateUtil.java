package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

  /**
   * Converts a submission period string into the last date of that month.
   *
   * @param submissionPeriod The submission period in format "MMM-yyyy" (e.g. "JAN-2026")
   * @param errorMessage The error message to display if the submissionPeriod string is blank
   * @return The last date of the specified month as a LocalDate
   * @throws DateTimeParseException if the submissionPeriod string cannot be parsed
   */
  public static LocalDate getLastDateOfMonth(String submissionPeriod, String errorMessage) {
    if (!StringUtils.hasText(submissionPeriod)) {
      throw new IllegalArgumentException(errorMessage);
    }
    YearMonth yearMonth = YearMonth.parse(submissionPeriod, SUBMISSION_PERIOD_FORMATTER);
    return yearMonth.atEndOfMonth();
  }
}
