package uk.gov.justice.laa.dstew.payments.claimsevent.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ContextUtil.assertContextClaimError;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ContextUtil.assertContextHasNoErrors;
import static uk.gov.justice.laa.dstew.payments.claimsevent.service.DuplicateClaimsTest.BULK_SUBMISSION_ID;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SubmissionValidationService;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

@ActiveProfiles("test")
@ImportTestcontainers(MessageListenerBase.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(MockServerIntegrationTest.ClaimsConfiguration.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.cloud.aws.sqs.enabled=false", // Disable AWS SQS functionality
      "laa.bulk-claim-queue.name=not-used", // Dummy queue name to avoid initialization issues,
    })
@DisplayName("Submission validation service integration tests")
public class SubmissionValidationServiceIntegrationTest extends MockServerIntegrationTest {

  private static final String OFFICE_CODE = "AQ2B3C";
  private static final AreaOfLaw AREA_OF_LAW = AreaOfLaw.LEGAL_HELP;

  @Autowired protected SubmissionValidationService submissionValidationService;

  UUID submissionId = UUID.fromString("0561d67b-30ed-412e-8231-f6296a53538d");

  @Nested
  @DisplayName("Submission period tests")
  class SubmissionPeriodTests {

    @Test
    @DisplayName("Should have no errors with submission period in the past")
    void shouldHaveNoErrorsWithSubmissionPeriodInThePast() throws Exception {
      // Given
      stubForGetSubmission(submissionId, "data-claims/get-submission/get-submission-APR-25.json");
      stubForUpdateSubmission(submissionId);
      stubReturnNoClaims(submissionId);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param("offices", OFFICE_CODE),
              Parameter.param("area_of_law", AREA_OF_LAW.name()),
              Parameter.param("submission_period", "APR-2025")),
          "data-claims/get-submission/get-submissions-by-filter_no_content.json");

      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(submissionId);

      // Then
      assertContextHasNoErrors(submissionValidationContext);
    }

    @Test
    @DisplayName("Should have one error with submission period before APR-2025")
    void shouldHaveOneErrorWithSubmissionPeriodIsBeforeMinimumPeriod() throws Exception {
      // Given
      stubForGetSubmission(submissionId, "data-claims/get-submission/get-submission-MAR-25.json");
      stubForUpdateSubmission(submissionId);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param("offices", OFFICE_CODE),
              Parameter.param("area_of_law", AREA_OF_LAW.name()),
              Parameter.param("submission_period", "MAR-2025")),
          "data-claims/get-submission/get-submissions-by-filter_no_content.json");

      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(submissionId);

      // Then
      assertThat(submissionValidationContext.getSubmissionValidationErrors().size()).isEqualTo(1);
      assertContextClaimError(
          submissionValidationContext,
          SubmissionValidationError.SUBMISSION_VALIDATION_MINIMUM_PERIOD,
          "APR-2025",
          "APR-2025");
    }

    @Test
    @DisplayName("Should have one error with submission period in same month as current")
    void shouldHaveOneErrorWithSubmissionPeriodInSameMonthAsCurrent() throws Exception {
      // Given
      stubForGetSubmission(submissionId, "data-claims/get-submission/get-submission-MAY-25.json");
      stubForUpdateSubmission(submissionId);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param("offices", OFFICE_CODE),
              Parameter.param("area_of_law", AREA_OF_LAW.name()),
              Parameter.param("submission_period", "MAY-2025")),
          "data-claims/get-submission/get-submissions-by-filter_no_content.json");

      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(submissionId);

      // Then
      assertThat(submissionValidationContext.getSubmissionValidationErrors().size()).isEqualTo(1);
      assertContextClaimError(
          submissionValidationContext,
          SubmissionValidationError.SUBMISSION_PERIOD_SAME_MONTH,
          "May 2025");
    }

    @Test
    @DisplayName("Should have one error with submission period in the future")
    void shouldHaveOneErrorWithSubmissionPeriodInTheFuture() throws Exception {
      // Given
      stubForGetSubmission(submissionId, "data-claims/get-submission/get-submission-SEP-25.json");
      stubForUpdateSubmission(submissionId);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param("offices", OFFICE_CODE),
              Parameter.param("area_of_law", AREA_OF_LAW.name()),
              Parameter.param("submission_period", "SEP-2025")),
          "data-claims/get-submission/get-submissions-by-filter_no_content.json");

      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(submissionId);

      // Then
      assertThat(submissionValidationContext.getSubmissionValidationErrors().size()).isEqualTo(1);
      assertContextClaimError(
          submissionValidationContext,
          SubmissionValidationError.SUBMISSION_PERIOD_FUTURE_MONTH,
          "May 2025");
    }

    @DisplayName(
        "Should have one error with submission period with combination of Office × Area of Law × Submission Period")
    @Test
    void shouldHaveOneErrorWithSubmissionPeriodWithCombinationOfOfficeAreaOfLawSubmissionPeriod()
        throws Exception {
      // Given
      stubForGetSubmission(submissionId, "data-claims/get-submission/get-submission-APR-25.json");
      stubForUpdateSubmission(submissionId);
      stubReturnNoClaims(submissionId);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param("offices", OFFICE_CODE),
              Parameter.param("area_of_law", AREA_OF_LAW.name()),
              Parameter.param("submission_period", "APR-2025")),
          "data-claims/get-submission/get-submissions-by-filter.json");

      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(submissionId);

      // Then
      assertThat(submissionValidationContext.getSubmissionValidationErrors().size()).isEqualTo(1);
      assertContextClaimError(
          submissionValidationContext,
          SubmissionValidationError.SUBMISSION_ALREADY_EXISTS,
          OFFICE_CODE,
          AREA_OF_LAW,
          "APR-2025");
    }
  }

  @Nested
  class SubmissionValidation {
    @DisplayName(
        "Should set the SubmissionValidationContext to valid message  when FSP return 404 for a claim")
    @Test
    void shouldSetSubmissionStatusToValidationFailedWhenFspReturn404ForAClaim() throws Exception {

      stubForGetSubmission(
          submissionId, "data-claims/get-submission/get-submission-with-claim.json");
      stubForUpdateSubmission(submissionId);
      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param("offices", OFFICE_CODE),
              Parameter.param("area_of_law", AREA_OF_LAW.name()),
              Parameter.param("submission_period", "APR-2025")),
          "data-claims/get-submission/get-submissions-by-filter_no_content.json");
      stubForGetClaim(
          submissionId,
          UUID.fromString("f6bde766-a0a3-483b-bf13-bef888b4f06e"),
          "data-claims/get-claim/get-claim-1.json");

      stubForGetClaims(Collections.emptyList(), "data-claims/get-claims/no-claims.json");

      stubForUpdateClaim(submissionId, UUID.fromString("f6bde766-a0a3-483b-bf13-bef888b4f06e"));

      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(
              new Parameter("areaOfLaw", AREA_OF_LAW.getValue()),
              new Parameter("effectiveDate", "14-08-2025")),
          "provider-details/get-firm-schedules-openapi-200.json");

      stubForPostFeeCalculationReturnError("fee-scheme/post-fee-calculation-200.json");

      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(submissionId);
      var validationMessagePatches =
          submissionValidationContext
              .getClaimReport("f6bde766-a0a3-483b-bf13-bef888b4f06e")
              .get()
              .getMessages();
      var filteredMessagePath =
          validationMessagePatches.stream()
              .filter(
                  validationMessagePatch ->
                      validationMessagePatch
                          .getDisplayMessage()
                          .equals(
                              ClaimValidationError.INVALID_FEE_CALCULATION_VALIDATION_FAILED
                                  .getDisplayMessage()))
              .toList();

      Assertions.assertFalse(filteredMessagePath.isEmpty());
    }
  }
}
