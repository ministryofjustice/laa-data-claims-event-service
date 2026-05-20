package uk.gov.justice.laa.dstew.payments.claimsevent.validator.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SubmissionValidationService;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

/**
 * Base class for submission validator integration tests.
 *
 * <p>A fixed "current month" of MAY-2026 is pinned via {@link MockitoBean} on {@link DateUtil} so
 * that period-based tests are deterministic regardless of when they run.
 */
@ActiveProfiles("test")
@ImportTestcontainers(MessageListenerBase.class)
@Import(MockServerIntegrationTest.ClaimsConfiguration.class)
@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.cloud.aws.sqs.enabled=false", "laa.bulk-claim-queue.name=not-used"})
public abstract class SubmissionValidationIntegrationTestBase extends MockServerIntegrationTest {

  /** Fixed "current month" used by all period-validator tests. */
  protected static final YearMonth PINNED_CURRENT_MONTH = YearMonth.of(2026, 5);

  protected static final String SUBMISSION_BASE_PATH = "data-claims/get-submission/";

  @Autowired protected SubmissionValidationService submissionValidationService;

  protected final ObjectMapper mapper = objectMapper;

  /** Pins {@link DateUtil#currentYearMonth()} to {@link #PINNED_CURRENT_MONTH}. */
  @MockitoBean protected DateUtil dateUtil;

  @BeforeAll
  void beforeAllBase() {
    // parent MockServerIntegrationTest.beforeAll will have run
  }

  @BeforeEach
  void resetBeforeEach() {
    mockServerClient.reset();
    Mockito.when(dateUtil.currentYearMonth()).thenReturn(PINNED_CURRENT_MONTH);
  }

  /**
   * Runs the full submission validation flow for the given fixture. Stubs all required MockServer
   * endpoints and returns the resulting {@link SubmissionValidationContext}.
   *
   * @param submissionFixture relative path under resources/responses/ to the submission JSON file
   * @param stubDuplicateSubmission when {@code true} stubs the criteria call to return a
   *     VALIDATION_SUCCEEDED match; when {@code false} returns no content
   */
  protected SubmissionValidationContext runSubmissionValidation(
      String submissionFixture, boolean stubDuplicateSubmission) throws Exception {

    String submissionJson = readJsonFromFile(submissionFixture);
    JsonNode node = mapper.readTree(submissionJson);

    UUID submissionId = UUID.fromString(node.get("submission_id").asText());
    UUID bulkSubmissionId = UUID.fromString(node.get("bulk_submission_id").asText());

    String officeAccountNumber =
        node.has("office_account_number") && !node.get("office_account_number").isNull()
            ? node.get("office_account_number").asText()
            : "AQ2B3C";

    AreaOfLaw areaOfLaw = AreaOfLaw.LEGAL_HELP;
    if (node.has("area_of_law") && !node.get("area_of_law").isNull()) {
      try {
        areaOfLaw =
            AreaOfLaw.valueOf(node.get("area_of_law").asText().replace(' ', '_').toUpperCase());
      } catch (Exception ignored) {
        // keep default
      }
    }

    String submissionPeriod =
        node.has("submission_period") && !node.get("submission_period").isNull()
            ? node.get("submission_period").asText()
            : "APR-2025";

    stubForGetSubmission(submissionId, submissionFixture);
    stubForUpdateSubmission(submissionId);
    stubForUpdateBulkSubmission(bulkSubmissionId);
    stubReturnNoClaims();

    String criteriaFixture =
        stubDuplicateSubmission
            ? SUBMISSION_BASE_PATH + "get-submissions-by-filter.json"
            : SUBMISSION_BASE_PATH + "get-submissions-by-filter_no_content.json";

    getStubForGetSubmissionByCriteria(
        List.of(
            Parameter.param("offices", officeAccountNumber),
            Parameter.param("area_of_law", areaOfLaw.name()),
            Parameter.param("submission_period", submissionPeriod)),
        criteriaFixture);

    return submissionValidationService.validateSubmission(submissionId);
  }

  /** Convenience overload — never stubs a duplicate (the common case). */
  protected SubmissionValidationContext runSubmissionValidation(String submissionFixture)
      throws Exception {
    return runSubmissionValidation(submissionFixture, false);
  }

  /**
   * Returns the submission-level errors as a list of {@link ValidationMessagePatch} for direct
   * inspection in tests.
   */
  protected List<ValidationMessagePatch> getSubmissionErrors(SubmissionValidationContext context) {
    return context.getSubmissionValidationErrors();
  }

  /** Asserts that there are no submission-level validation errors. */
  protected void assertNoSubmissionErrors(SubmissionValidationContext context) {
    List<ValidationMessagePatch> errors = getSubmissionErrors(context);
    assertTrue(
        errors.isEmpty(),
        "Expected no submission errors but got: "
            + errors.stream().map(ValidationMessagePatch::getDisplayMessage).toList());
  }

  /**
   * Asserts that the submission-level errors contain exactly the expected error count and that each
   * expected {@link SubmissionValidationError} is represented. Matching is done by checking that
   * the actual display message starts with the non-parameterised prefix of the enum's message
   * template.
   *
   * @param context the validation context to inspect
   * @param expected the expected errors (order-independent)
   */
  protected void assertSubmissionErrors(
      SubmissionValidationContext context, Set<SubmissionValidationError> expected) {

    List<ValidationMessagePatch> actual = getSubmissionErrors(context);

    // Build the set of actual display messages
    Set<String> actualMessages =
        actual.stream().map(ValidationMessagePatch::getDisplayMessage).collect(Collectors.toSet());

    // For each expected error, verify at least one actual message starts with its base prefix
    for (SubmissionValidationError expectedError : expected) {
      String prefix = messagePrefix(expectedError);
      boolean found = actualMessages.stream().anyMatch(m -> m != null && m.startsWith(prefix));
      assertTrue(
          found,
          "Expected submission error ["
              + expectedError.name()
              + "] (prefix: \""
              + prefix
              + "\") not found in: "
              + actualMessages);
    }

    assertEquals(
        expected.size(),
        actual.size(),
        "Expected exactly "
            + expected.size()
            + " submission error(s) but got "
            + actual.size()
            + ": "
            + actualMessages);
  }

  /**
   * Extracts the static (non-parameterised) prefix of an error's display message template by taking
   * everything up to the first {@code %} placeholder.
   */
  private String messagePrefix(SubmissionValidationError error) {
    String template = error.getDisplayMessage();
    int idx = template.indexOf('%');
    return idx >= 0 ? template.substring(0, idx).trim() : template.trim();
  }
}
