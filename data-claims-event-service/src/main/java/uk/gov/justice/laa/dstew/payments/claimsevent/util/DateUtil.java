package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Utility class to support date-related operations in the claims event service. Provides
 * functionality for date formatting, parsing, and submission period resolution.
 *
 * <p>The class supports standard date formats used across the service.
 */
@Slf4j
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

  /**
   * Parses the given submission period string into a {@link YearMonth}.
   *
   * @param submissionPeriod the submission period string in the format "MMM-yyyy" (e.g. "JAN-2026")
   * @return the parsed {@link YearMonth}, or {@code null} if the value is blank, absent, or not a
   *     valid submission period
   */
  public static YearMonth parseSubmissionPeriod(String submissionPeriod) {
    if (!StringUtils.hasText(submissionPeriod)) {
      return null;
    }
    try {
      return YearMonth.parse(submissionPeriod, SUBMISSION_PERIOD_FORMATTER);
    } catch (Exception e) {
      log.warn("Could not parse submission period '{}'", submissionPeriod);
      return null;
    }
  }

  /** Gets the current date as a {@link YearMonth} object. */
  public YearMonth currentYearMonth() {
    return YearMonth.now();
  }
}
