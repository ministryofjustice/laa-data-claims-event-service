package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import java.time.YearMonth;
import org.springframework.stereotype.Component;

/**
 * Utility class for date-related operations. Acts as a wrapper around common date operations for
 * easier testing.
 */
@Component
public class DateUtil {

  /** Gets the current date as a {@link YearMonth} object. */
  public YearMonth currentYearMonth() {
    return YearMonth.now();
  }
}
