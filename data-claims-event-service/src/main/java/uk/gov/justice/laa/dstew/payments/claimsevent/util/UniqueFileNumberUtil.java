package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;

/**
 * Utility class for parsing a unique file number.
 *
 * @author Jamie Briggs
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UniqueFileNumberUtil {

  /**
   * Parses a unique file number into a {@link LocalDate} object.
   *
   * @param uniqueFileNumber the unique file number to parse, should be in format ddMMyy/NNN.
   * @return the parsed {@link LocalDate} object.
   */
  public static LocalDate parse(String uniqueFileNumber) {
    if (uniqueFileNumber == null || !uniqueFileNumber.matches("\\d{6}/\\d{3}")) {
      throw new EventServiceIllegalArgumentException(
          String.format(
              "Invalid format for unique file number: %s. Expected format: ddMMyy/NNN",
              uniqueFileNumber));
    }

    String datePart = uniqueFileNumber.split("/")[0];

    int day = Integer.parseInt(datePart.substring(0, 2));
    int month = Integer.parseInt(datePart.substring(2, 4));
    int yearTwoDigits = Integer.parseInt(datePart.substring(4, 6));

    int year = (yearTwoDigits >= 50) ? 1900 + yearTwoDigits : 2000 + yearTwoDigits;

    return LocalDate.of(year, month, day);
  }
}
