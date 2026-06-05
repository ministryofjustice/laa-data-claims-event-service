package uk.gov.justice.laa.dstew.payments.claimsevent.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.ValidationIssue;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.ValidationResult;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.service.ValidationService;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.util.DateUtils;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validator.submission.SubmissionValidationIntegrationTestBase;

/**
 * Integration tests asserting parity between the legacy (old) submission validators and the shared
 * (new) {@link ValidationService#validateSubmission(SubmissionResponse)} for representative
 * submission scenarios.
 *
 * <p>For each scenario both pipelines are exercised against the same fixture and their outputs are
 * compared strictly:
 *
 * <ul>
 *   <li>Same number of issues / errors produced
 *   <li>Each shared-validator issue has a matching legacy error with identical display message,
 *       technical message and severity/type
 * </ul>
 *
 * <p>The shared validator's {@link DateUtils} clock is pinned to the same {@link
 * #PINNED_CURRENT_MONTH} (MAY-2026) used by the legacy {@link DateUtil} mock so that
 * period-sensitive tests are deterministic.
 *
 * <p>If a test fails it means a divergence has been detected between the two pipelines. The
 * validator package is assumed to be the source of the discrepancy and should be resolved
 * separately — do not modify this test to suppress failures.
 */
@DisplayName("Shared submission validation parity integration tests")
public class SharedSubmissionValidationParityIntegrationTest
    extends SubmissionValidationIntegrationTestBase {

  /**
   * Pin {@link DateUtils}'s static clock to MAY-2026 so the shared validator uses the same "current
   * month" as the mocked {@link DateUtil} in the base class.
   */
  @BeforeEach
  void pinSharedValidatorClock() {
    DateUtils.setClock(
        Clock.fixed(
            PINNED_CURRENT_MONTH.atDay(15).atStartOfDay(ZoneOffset.UTC).toInstant(),
            ZoneOffset.UTC));
  }

  @AfterEach
  void resetSharedValidatorClock() {
    DateUtils.resetClock();
  }

  // ── Valid submission ──────────────────────────────────────────────────────

  @Test
  @DisplayName(
      "Parity: valid submission (MAR-2026 past period, READY_FOR_VALIDATION) - both produce no errors")
  void parityValidSubmissionProducesNoErrors() throws Exception {
    var fixture = SUBMISSION_BASE_PATH + "SubmissionPeriodValidator/period-valid-past.json";
    assertParity(runSubmissionValidation(fixture), parseSubmissionFromFixture(fixture));
  }

  // ── Schema violation ──────────────────────────────────────────────────────

  @Test
  @DisplayName(
      "Parity: schema violation (missing is_nil_submission) - both produce the same schema error")
  void paritySchemaViolationMissingIsNilSubmission() throws Exception {
    var fixture = SUBMISSION_BASE_PATH + "SubmissionSchemaValidator/missing-is-nil-submission.json";
    assertParity(runSubmissionValidation(fixture), parseSubmissionFromFixture(fixture));
  }

  // ── Period violations ─────────────────────────────────────────────────────

  @Test
  @DisplayName(
      "Parity: submission period same as current month (MAY-2026) - both produce SUBMISSION_PERIOD_SAME_MONTH")
  void parityPeriodViolationSameMonth() throws Exception {
    var fixture = SUBMISSION_BASE_PATH + "SubmissionPeriodValidator/period-same-month.json";
    assertParity(runSubmissionValidation(fixture), parseSubmissionFromFixture(fixture));
  }

  @Test
  @DisplayName(
      "Parity: submission period in the future (JUN-2026) - both produce SUBMISSION_PERIOD_FUTURE_MONTH")
  void parityPeriodViolationFutureMonth() throws Exception {
    var fixture = SUBMISSION_BASE_PATH + "SubmissionPeriodValidator/period-future.json";
    assertParity(runSubmissionValidation(fixture), parseSubmissionFromFixture(fixture));
  }

  @Test
  @DisplayName(
      "Parity: submission period before minimum (MAR-2025) - both produce SUBMISSION_VALIDATION_MINIMUM_PERIOD")
  void parityPeriodViolationBeforeMinimum() throws Exception {
    var fixture = SUBMISSION_BASE_PATH + "SubmissionPeriodValidator/period-before-minimum.json";
    assertParity(runSubmissionValidation(fixture), parseSubmissionFromFixture(fixture));
  }

  // ── Nil submission cases ──────────────────────────────────────────────────

  @Test
  @DisplayName(
      "Parity: nil submission that contains claims - both produce INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS")
  void parityNilSubmissionWithClaims() throws Exception {
    var fixture = SUBMISSION_BASE_PATH + "NilSubmissionValidator/nil-submission-with-claims.json";
    assertParity(runSubmissionValidation(fixture), parseSubmissionFromFixture(fixture));
  }

  @Test
  @DisplayName(
      "Parity: non-nil submission with no claims - both produce NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS")
  void parityNonNilSubmissionNoClaims() throws Exception {
    var fixture = SUBMISSION_BASE_PATH + "NilSubmissionValidator/non-nil-submission-no-claims.json";
    assertParity(runSubmissionValidation(fixture), parseSubmissionFromFixture(fixture));
  }

  // ── Duplicate office / area of law / period ───────────────────────────────

  @Test
  @DisplayName(
      "Parity: duplicate office/area/period combination - both produce SUBMISSION_ALREADY_EXISTS")
  void parityDuplicateOfficeAreaPeriod() throws Exception {
    var fixture =
        SUBMISSION_BASE_PATH
            + "SubmissionOfficeAreaOfLawAndPeriodValidator/duplicate-submission.json";
    // stubDuplicateSubmission = true: MockServer returns a VALIDATION_SUCCEEDED match
    assertParity(runSubmissionValidation(fixture, true), parseSubmissionFromFixture(fixture));
  }

  // ── Incorrect status ──────────────────────────────────────────────────────

  @Test
  @DisplayName(
      "Parity: incorrect submission status (VALIDATION_SUCCEEDED) - both produce INCORRECT_SUBMISSION_STATUS_FOR_VALIDATION")
  void parityIncorrectStatus() throws Exception {
    var fixture = SUBMISSION_BASE_PATH + "SubmissionStatusValidator/status-incorrect.json";
    assertParity(runSubmissionValidation(fixture), parseSubmissionFromFixture(fixture));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /**
   * Calls the shared validator directly and asserts strict parity with the legacy context:
   *
   * <ol>
   *   <li>Same issue / error count
   *   <li>Each shared issue has an exact match in the legacy errors by display message, technical
   *       message and severity / type
   * </ol>
   */
  private void assertParity(SubmissionValidationContext context, SubmissionResponse submission) {
    ValidationResult newResult = validationService.validateSubmission(submission);
    List<ValidationIssue> newIssues =
        (newResult == null || newResult.getIssues() == null) ? List.of() : newResult.getIssues();
    List<ValidationMessagePatch> oldErrors = context.getSubmissionValidationErrors();

    assertThat(newIssues)
        .as(
            """
            Shared and legacy validators must produce the same number of submission errors.
              Shared issues : %s
              Legacy errors : %s""",
            newIssues.stream().map(ValidationIssue::getMessage).toList(),
            oldErrors.stream().map(ValidationMessagePatch::getDisplayMessage).toList())
        .hasSameSizeAs(oldErrors);

    for (ValidationIssue issue : newIssues) {
      boolean matched =
          oldErrors.stream()
              .anyMatch(
                  patch ->
                      Objects.equals(patch.getDisplayMessage(), issue.getMessage())
                          && Objects.equals(
                              patch.getTechnicalMessage(), issue.getTechnicalMessage())
                          && Objects.equals(
                              patch.getType() == null ? null : patch.getType().name(),
                              issue.getSeverity() == null ? null : issue.getSeverity().name()));

      assertThat(matched)
          .as(
              "Shared validator issue [code=%s, message='%s'] has no matching entry in legacy errors.\n"
                  + "  Legacy errors: %s",
              issue.getCode(),
              issue.getMessage(),
              oldErrors.stream().map(ValidationMessagePatch::getDisplayMessage).toList())
          .isTrue();
    }
  }

  /**
   * Deserialises the submission fixture at {@code fixtureRelativePath} into a {@link
   * SubmissionResponse} using the test's pre-configured {@link #objectMapper} (includes {@code
   * JavaTimeModule} for {@code OffsetDateTime} fields).
   */
  private SubmissionResponse parseSubmissionFromFixture(String fixtureRelativePath)
      throws Exception {
    return objectMapper.readValue(readJsonFromFile(fixtureRelativePath), SubmissionResponse.class);
  }
}
