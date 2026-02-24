package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.YearMonth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

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

  @Nested
  @DisplayName("currentYearMonth")
  class CurrentYearMonth {

    @Test
    @DisplayName("Should return a non-null YearMonth representing the current month")
    void shouldReturnCurrentYearMonth() {
      assertThat(new DateUtil().currentYearMonth()).isEqualTo(YearMonth.now());
    }
  }
}
