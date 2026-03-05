package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;

@DisplayName("DateUtil")
class DateUtilTest {

  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void attachLogAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(DateUtil.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void detachLogAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(DateUtil.class);
    logger.detachAppender(logAppender);
  }

  @Nested
  @DisplayName("parseSubmissionPeriod")
  class ParseSubmissionPeriod {

    @ParameterizedTest(name = "returns null for blank input: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Should return null when the submission period is null, empty, or blank")
    void shouldReturnNullWhenBlankOrNull(String input) {
      assertThat(DateUtil.parseSubmissionPeriod(input)).isNull();
    }

    @Test
    @DisplayName("Should parse a valid uppercase submission period")
    void shouldParseUppercaseSubmissionPeriod() {
      assertThat(DateUtil.parseSubmissionPeriod("JAN-2026")).isEqualTo(YearMonth.of(2026, 1));
    }

    @Test
    @DisplayName("Should parse a valid lowercase submission period (case-insensitive)")
    void shouldParseLowercaseSubmissionPeriod() {
      assertThat(DateUtil.parseSubmissionPeriod("jan-2026")).isEqualTo(YearMonth.of(2026, 1));
    }

    @Test
    @DisplayName("Should parse a valid mixed-case submission period (case-insensitive)")
    void shouldParseMixedCaseSubmissionPeriod() {
      assertThat(DateUtil.parseSubmissionPeriod("Jan-2026")).isEqualTo(YearMonth.of(2026, 1));
    }

    @Test
    @DisplayName("Should parse all valid month values across a year boundary")
    void shouldParseAllMonths() {
      assertThat(DateUtil.parseSubmissionPeriod("DEC-2025")).isEqualTo(YearMonth.of(2025, 12));
      assertThat(DateUtil.parseSubmissionPeriod("JUN-2024")).isEqualTo(YearMonth.of(2024, 6));
    }

    @Test
    @DisplayName("Should return null and log a warning when the submission period is unparseable")
    void shouldReturnNullAndLogWarnWhenUnparseable() {
      assertThat(DateUtil.parseSubmissionPeriod("not-a-period")).isNull();

      assertThat(logAppender.list)
          .anySatisfy(
              event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("not-a-period");
              });
    }

    @Test
    @DisplayName("Should return null and log a warning when the month abbreviation is invalid")
    void shouldReturnNullAndLogWarnWhenMonthAbbreviationIsInvalid() {
      assertThat(DateUtil.parseSubmissionPeriod("ABC-2026")).isNull();

      assertThat(logAppender.list)
          .anySatisfy(
              event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("ABC-2026");
              });
    }

    @Test
    @DisplayName("Should return null and log a warning when the year portion is missing")
    void shouldReturnNullAndLogWarnWhenYearIsMissing() {
      assertThat(DateUtil.parseSubmissionPeriod("JAN")).isNull();

      assertThat(logAppender.list)
          .anySatisfy(
              event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("JAN");
              });
    }

    @Test
    @DisplayName("Should not log any warnings when the submission period is valid")
    void shouldNotLogWarnWhenValid() {
      DateUtil.parseSubmissionPeriod("MAR-2026");

      assertThat(logAppender.list)
          .noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.WARN));
    }
  }

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
    @DisplayName("Should return a non-null YearMonth representing the current month")
    void shouldReturnCurrentYearMonth() {
      assertThat(new DateUtil().currentYearMonth()).isEqualTo(YearMonth.now());
    }

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
