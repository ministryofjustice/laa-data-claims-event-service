package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;

@DisplayName("DateUtil")
class DateUtilTest {

  // ---------------------------------------------------------------------------
  // parseDate
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("parseDate")
  class ParseDate {

    static Stream<Arguments> validDates() {
      return Stream.of(
          Arguments.of("2025-01-01", LocalDate.of(2025, 1, 1), "first day of year"),
          Arguments.of("2025-12-31", LocalDate.of(2025, 12, 31), "last day of year"),
          Arguments.of("2000-02-29", LocalDate.of(2000, 2, 29), "leap day"),
          Arguments.of("2026-11-20", LocalDate.of(2026, 11, 20), "typical date"));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("validDates")
    @DisplayName("Returns correct LocalDate for valid yyyy-MM-dd input")
    @SuppressWarnings("unused")
    void returnsCorrectLocalDate(String input, LocalDate expected, String description) {
      assertThat(DateUtil.parseDate(input, "test field")).isEqualTo(expected);
    }

    @ParameterizedTest(name = "Throws for invalid input [{0}]")
    @ValueSource(
        strings = {
          "not-a-date",
          "01-01-2025",
          "2025/01/01",
          "20250101",
          "2025-13-01",
          "2025-00-01"
        })
    @DisplayName("Throws EventServiceIllegalArgumentException for unparseable input")
    void throwsForUnparseableInput(String input) {
      assertThatThrownBy(() -> DateUtil.parseDate(input, "test field"))
          .isInstanceOf(EventServiceIllegalArgumentException.class)
          .hasMessageContaining("test field")
          .hasMessageContaining(input);
    }

    @Test
    @DisplayName("Exception message includes the field name supplied by the caller")
    void exceptionMessageIncludesFieldName() {
      assertThatThrownBy(() -> DateUtil.parseDate("bad", "case start date"))
          .isInstanceOf(EventServiceIllegalArgumentException.class)
          .hasMessageContaining("case start date");
    }
  }

  // ---------------------------------------------------------------------------
  // currentYearMonth
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("currentYearMonth")
  class CurrentYearMonth {

    @Test
    @DisplayName("Returns a non-null YearMonth")
    void returnsNonNull() {
      assertThat(new DateUtil().currentYearMonth()).isNotNull();
    }

    @Test
    @DisplayName("Returns the current year and month")
    void returnsCurrentYearAndMonth() {
      YearMonth now = YearMonth.now();
      // Allow one-month tolerance in case the test runs at a month boundary
      YearMonth result = new DateUtil().currentYearMonth();
      assertThat(result).isBetween(now.minusMonths(1), now.plusMonths(1));
    }
  }

  // ---------------------------------------------------------------------------
  // DATE_FORMATTER_YYYY_MM_DD
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("DATE_FORMATTER_YYYY_MM_DD")
  class DateFormatterYyyyMmDd {

    @Test
    @DisplayName("Formats a LocalDate as yyyy-MM-dd")
    void formatsDate() {
      assertThat(LocalDate.of(2025, 6, 1).format(DateUtil.DATE_FORMATTER_YYYY_MM_DD))
          .isEqualTo("2025-06-01");
    }

    @Test
    @DisplayName("Parses a yyyy-MM-dd string to a LocalDate")
    void parsesDate() {
      assertThat(LocalDate.parse("2025-06-01", DateUtil.DATE_FORMATTER_YYYY_MM_DD))
          .isEqualTo(LocalDate.of(2025, 6, 1));
    }
  }

  // ---------------------------------------------------------------------------
  // DATE_FORMATTER_FOR_DISPLAY_MESSAGE
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("DATE_FORMATTER_FOR_DISPLAY_MESSAGE")
  class DateFormatterForDisplayMessage {

    @Test
    @DisplayName("Formats a LocalDate as dd/MM/yyyy")
    void formatsDate() {
      assertThat(LocalDate.of(2025, 6, 1).format(DateUtil.DATE_FORMATTER_FOR_DISPLAY_MESSAGE))
          .isEqualTo("01/06/2025");
    }

    @Test
    @DisplayName("Parses a dd/MM/yyyy string to a LocalDate")
    void parsesDate() {
      assertThat(LocalDate.parse("01/06/2025", DateUtil.DATE_FORMATTER_FOR_DISPLAY_MESSAGE))
          .isEqualTo(LocalDate.of(2025, 6, 1));
    }
  }

  // ---------------------------------------------------------------------------
  // SUBMISSION_PERIOD_FORMATTER
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("SUBMISSION_PERIOD_FORMATTER")
  class SubmissionPeriodFormatter {

    static Stream<Arguments> submissionPeriodCases() {
      return Stream.of(
          Arguments.of("JAN-2026", YearMonth.of(2026, 1), "uppercase month"),
          Arguments.of("jan-2026", YearMonth.of(2026, 1), "lowercase month"),
          Arguments.of("Jan-2026", YearMonth.of(2026, 1), "mixed case month"),
          Arguments.of("DEC-2025", YearMonth.of(2025, 12), "December"),
          Arguments.of("FEB-2026", YearMonth.of(2026, 2), "February"));
    }

    @ParameterizedTest(name = "Parses \"{0}\" — {2}")
    @MethodSource("submissionPeriodCases")
    @DisplayName("Parses MMM-yyyy format case-insensitively")
    @SuppressWarnings("unused")
    void parsesSubmissionPeriod(String input, YearMonth expected, String description) {
      assertThat(YearMonth.parse(input, DateUtil.SUBMISSION_PERIOD_FORMATTER)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Formats a YearMonth as MMM-yyyy in uppercase English")
    void formatsSubmissionPeriod() {
      assertThat(YearMonth.of(2026, 1).format(DateUtil.SUBMISSION_PERIOD_FORMATTER))
          .isEqualToIgnoringCase("JAN-2026");
    }
  }
}
