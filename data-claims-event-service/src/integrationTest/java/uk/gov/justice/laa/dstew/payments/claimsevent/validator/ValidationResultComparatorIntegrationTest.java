package uk.gov.justice.laa.dstew.payments.claimsevent.validator;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.ValidationIssue;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.ValidationSeverity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SubmissionValidationService;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.ValidationResultComparator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validator.submission.SubmissionValidationIntegrationTestBase;

/**
 * Integration tests for {@link ValidationResultComparator}.
 *
 * <p>Verifies two concerns:
 *
 * <ol>
 *   <li><b>Structural</b> — the comparator path inside {@link SubmissionValidationService} is
 *       executed for real submissions and does <em>not</em> mutate the {@link
 *       SubmissionValidationContext} returned to callers.
 *   <li><b>Logging</b> — the comparator emits the expected WARN / DEBUG log signals when invoked
 *       directly with mismatched or matched issue lists.
 * </ol>
 */
@DisplayName("ValidationResultComparator integration tests")
public class ValidationResultComparatorIntegrationTest
    extends SubmissionValidationIntegrationTestBase {

  private static final String SUBMISSION_LABEL = "Submission test-id";

  private ListAppender<ILoggingEvent> logAppender;
  private Logger comparatorLogger;

  @BeforeEach
  void setUpLogCapture() {
    comparatorLogger = (Logger) LoggerFactory.getLogger(ValidationResultComparator.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    comparatorLogger.addAppender(logAppender);
    // Capture DEBUG so we can verify the comparator ran (not just WARN)
    comparatorLogger.setLevel(Level.DEBUG);
  }

  @AfterEach
  void tearDownLogCapture() {
    comparatorLogger.detachAppender(logAppender);
    comparatorLogger.setLevel(null); // Reset to inherited level
  }

  // ── Structural: comparator runs inside SubmissionValidationService ───────

  @Test
  @DisplayName(
      "Comparator is executed during validation and does not mutate context for a valid submission")
  void comparatorDoesNotMutateContextForValidSubmission() throws Exception {
    var ctx =
        runSubmissionValidation(
            SUBMISSION_BASE_PATH + "SubmissionStatusValidator/status-ready-for-validation.json");

    assertNoSubmissionErrors(ctx);

    // Verify the comparator actually ran — it always emits at least one DEBUG log
    assertThat(logAppender.list)
        .as("Expected at least one log entry from ValidationResultComparator")
        .isNotEmpty();
  }

  @Test
  @DisplayName(
      "Comparator is executed and context contains exactly one error after running for an incorrect-status submission")
  void comparatorDoesNotMutateContextForSubmissionWithError() throws Exception {
    var ctx =
        runSubmissionValidation(
            SUBMISSION_BASE_PATH + "SubmissionStatusValidator/status-incorrect.json");

    // The comparator must not inflate or deflate the error count
    assertThat(ctx.getSubmissionValidationErrors())
        .as("Context should contain exactly one submission error after the comparator ran")
        .hasSize(1);
    assertThat(ctx.getSubmissionValidationErrors().getFirst().getDisplayMessage())
        .startsWith("Submission cannot be validated in state");

    // Comparator must have run
    assertThat(logAppender.list)
        .as("Expected at least one log entry from ValidationResultComparator")
        .isNotEmpty();
  }

  // ── Logging: WARN path (mismatch) ────────────────────────────────────────

  @Test
  @DisplayName(
      "Comparator logs WARN '[VALIDATOR-DRY_RUN] only in new validator' when new issue has no matching old error")
  void comparatorLogsWarnWhenNewIssueAbsentFromOldErrors() {
    ValidationIssue extraNew =
        ValidationIssue.builder()
            .code("EXTRA_NEW")
            .message("An issue only in the new validator")
            .severity(ValidationSeverity.ERROR)
            .technicalMessage(null)
            .build();

    ValidationResultComparator.compare(SUBMISSION_LABEL, List.of(extraNew), List.of());

    assertThat(logAppender.list)
        .as("Expected a WARN log for the unmatched new issue")
        .anyMatch(
            e ->
                e.getLevel() == Level.WARN
                    && e.getFormattedMessage().contains("[VALIDATOR-DRY-RUN]")
                    && e.getFormattedMessage().contains("only in new validator"));
  }

  @Test
  @DisplayName(
      "Comparator logs WARN '[VALIDATOR-DRY-RUN] only in existing validator' when old error has no matching new issue")
  void comparatorLogsWarnWhenOldErrorAbsentFromNewIssues() {
    ValidationMessagePatch extraOld =
        new ValidationMessagePatch()
            .displayMessage("An error only in the old validator")
            .technicalMessage(null)
            .type(ValidationMessageType.ERROR)
            .source("Data-Claims-Event-Service");

    ValidationResultComparator.compare(SUBMISSION_LABEL, List.of(), List.of(extraOld));

    assertThat(logAppender.list)
        .as("Expected a WARN log for the unmatched old error")
        .anyMatch(
            e ->
                e.getLevel() == Level.WARN
                    && e.getFormattedMessage().contains("[VALIDATOR-DRY-RUN]")
                    && e.getFormattedMessage().contains("only in existing validator"));
  }

  // ── Logging: no WARN path (match / both empty) ───────────────────────────

  @Test
  @DisplayName("Comparator does not log WARN when both validators produce no issues")
  void comparatorDoesNotLogWarnWhenBothProduceNoIssues() {
    ValidationResultComparator.compare(SUBMISSION_LABEL, List.of(), List.of());

    assertThat(logAppender.list)
        .as("No WARN expected when both sides are empty")
        .noneMatch(e -> e.getLevel() == Level.WARN);
  }

  @Test
  @DisplayName(
      "Comparator does not log WARN when new issue exactly matches old error by message, technical message and severity")
  void comparatorDoesNotLogWarnWhenIssueExactlyMatchesOldError() {
    String sharedMessage = "Matching validation message";

    ValidationIssue newIssue =
        ValidationIssue.builder()
            .code("MATCH_CODE")
            .message(sharedMessage)
            .technicalMessage(null)
            .severity(ValidationSeverity.ERROR)
            .build();

    ValidationMessagePatch oldError =
        new ValidationMessagePatch()
            .displayMessage(sharedMessage)
            .technicalMessage(null)
            .type(ValidationMessageType.ERROR);

    ValidationResultComparator.compare(SUBMISSION_LABEL, List.of(newIssue), List.of(oldError));

    assertThat(logAppender.list)
        .as("No WARN expected for an exact match")
        .noneMatch(e -> e.getLevel() == Level.WARN);
  }
}
