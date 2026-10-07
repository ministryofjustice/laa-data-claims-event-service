package uk.gov.justice.laa.dstew.payments.claimsevent.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ContextUtil.assertContextClaimError;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ContextUtil.assertContextHasNoErrors;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;
import uk.gov.justice.laa.dstew.payments.claimsevent.ContextUtil;
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
  private static final AreaOfLaw LEGAL_HELP = AreaOfLaw.LEGAL_HELP;
  public static final String SUBMISSION_PERIOD_PARAM = "submission_period";
  public static final String SUBMISSIONS_BY_FILTER_NO_CONTENT_JSON =
      "data-claims/get-submission/get-submissions-by-filter_no_content.json";
  public static final String GET_SUBMISSION_APR_25_JSON =
      "data-claims/get-submission/get-submission-APR-25.json";

  @Autowired protected SubmissionValidationService submissionValidationService;

  private static final UUID SUBMISSION_ID = UUID.fromString("0561d67b-30ed-412e-8231-f6296a53538d");
  private static final UUID BULK_SUBMISSION_ID =
      UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
  private static final String CLAIM_ID = "f6bde766-a0a3-483b-bf13-bef888b4f06e";
  public static final String OFFICES_PARAM = "offices";
  public static final String AREA_OF_LAW_PARAM = "area_of_law";
  public static final String APR_2025 = "APR-2025";

  @Nested
  @DisplayName("Submission period tests")
  class SubmissionPeriodTests {

    @Test
    @DisplayName("Should have no errors with submission period in the past")
    void shouldHaveNoErrorsWithSubmissionPeriodInThePast() throws Exception {
      // Given
      stubForGetSubmission(SUBMISSION_ID, GET_SUBMISSION_APR_25_JSON);
      stubForUpdateSubmission(SUBMISSION_ID);
      stubReturnNoClaims();
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param(OFFICES_PARAM, OFFICE_CODE),
              Parameter.param(AREA_OF_LAW_PARAM, LEGAL_HELP.name()),
              Parameter.param(SUBMISSION_PERIOD_PARAM, APR_2025)),
          SUBMISSIONS_BY_FILTER_NO_CONTENT_JSON);

      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(SUBMISSION_ID);

      // Then
      assertContextHasNoErrors(submissionValidationContext);
    }

    @Test
    @DisplayName("Should have one error with submission period before APR-2025")
    void shouldHaveOneErrorWithSubmissionPeriodIsBeforeMinimumPeriod() throws Exception {
      // Given
      stubForGetSubmission(SUBMISSION_ID, "data-claims/get-submission/get-submission-MAR-25.json");
      stubForUpdateSubmission(SUBMISSION_ID);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param(OFFICES_PARAM, OFFICE_CODE),
              Parameter.param(AREA_OF_LAW_PARAM, LEGAL_HELP.name()),
              Parameter.param(SUBMISSION_PERIOD_PARAM, "MAR-2025")),
          SUBMISSIONS_BY_FILTER_NO_CONTENT_JSON);

      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(SUBMISSION_ID);

      // Then
      assertThat(submissionValidationContext.getSubmissionValidationErrors().size()).isEqualTo(1);
      assertContextClaimError(
          submissionValidationContext,
          SubmissionValidationError.SUBMISSION_VALIDATION_MINIMUM_PERIOD,
          APR_2025,
          APR_2025);
    }

    @Test
    @DisplayName("Should have one error with submission period in same month as current")
    void shouldHaveOneErrorWithSubmissionPeriodInSameMonthAsCurrent() throws Exception {
      // Given
      stubForGetSubmission(SUBMISSION_ID, "data-claims/get-submission/get-submission-MAY-25.json");
      stubForUpdateSubmission(SUBMISSION_ID);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param(OFFICES_PARAM, OFFICE_CODE),
              Parameter.param(AREA_OF_LAW_PARAM, LEGAL_HELP.name()),
              Parameter.param(SUBMISSION_PERIOD_PARAM, "MAY-2025")),
          SUBMISSIONS_BY_FILTER_NO_CONTENT_JSON);

      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(SUBMISSION_ID);

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
      stubForGetSubmission(SUBMISSION_ID, "data-claims/get-submission/get-submission-SEP-25.json");
      stubForUpdateSubmission(SUBMISSION_ID);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param(OFFICES_PARAM, OFFICE_CODE),
              Parameter.param(AREA_OF_LAW_PARAM, LEGAL_HELP.name()),
              Parameter.param(SUBMISSION_PERIOD_PARAM, "SEP-2025")),
          SUBMISSIONS_BY_FILTER_NO_CONTENT_JSON);

      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(SUBMISSION_ID);

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
      stubForGetSubmission(SUBMISSION_ID, GET_SUBMISSION_APR_25_JSON);
      stubForUpdateSubmission(SUBMISSION_ID);
      stubReturnNoClaims();
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param(OFFICES_PARAM, OFFICE_CODE),
              Parameter.param(AREA_OF_LAW_PARAM, LEGAL_HELP.name()),
              Parameter.param(SUBMISSION_PERIOD_PARAM, APR_2025)),
          "data-claims/get-submission/get-submissions-by-filter.json");

      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(SUBMISSION_ID);

      // Then
      assertThat(submissionValidationContext.getSubmissionValidationErrors().size()).isEqualTo(1);
      assertContextClaimError(
          submissionValidationContext,
          SubmissionValidationError.SUBMISSION_ALREADY_EXISTS,
          OFFICE_CODE,
          LEGAL_HELP,
          APR_2025);
    }

    @DisplayName(
        "Should have no error when the only matching submission was created after the one under validation")
    @Test
    void shouldHaveNoErrorWhenMatchingSubmissionWasCreatedLater() throws Exception {
      // Given
      stubForGetSubmission(SUBMISSION_ID, GET_SUBMISSION_APR_25_JSON);
      stubForUpdateSubmission(SUBMISSION_ID);
      stubReturnNoClaims();
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param(OFFICES_PARAM, OFFICE_CODE),
              Parameter.param(AREA_OF_LAW_PARAM, LEGAL_HELP.name()),
              Parameter.param(SUBMISSION_PERIOD_PARAM, APR_2025)),
          "data-claims/get-submission/get-submissions-by-filter-later-duplicate.json");

      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(SUBMISSION_ID);

      // Then
      assertContextHasNoErrors(submissionValidationContext);
    }
  }

  @Nested
  class SubmissionValidation {
    @DisplayName(
        "Should set the SubmissionValidationContext to valid message when FSP return 404 for a claim")
    @Test
    void shouldSetSubmissionStatusToValidationFailedWhenFspReturn404ForAClaim() throws Exception {

      stubForGetSubmission(
          SUBMISSION_ID, "data-claims/get-submission/get-submission-with-claim.json");
      stubForUpdateSubmission(SUBMISSION_ID);
      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param(OFFICES_PARAM, OFFICE_CODE),
              Parameter.param(AREA_OF_LAW_PARAM, LEGAL_HELP.name()),
              Parameter.param(SUBMISSION_PERIOD_PARAM, APR_2025)),
          SUBMISSIONS_BY_FILTER_NO_CONTENT_JSON);
      stubForGetClaims(Collections.emptyList(), "data-claims/get-claims/claim-two-claims.json");

      stubForGetClaims(Collections.emptyList(), "data-claims/get-claims/no-claims.json");

      stubForUpdateClaim(SUBMISSION_ID, UUID.fromString(CLAIM_ID));

      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(
              new Parameter("effectiveDate", "14-08-2025"),
              new Parameter("requireOpenStatus", "false")),
          "provider-details/get-firm-schedules-openapi-200.json");

      stubForPostFeeCalculationReturnError("fee-scheme/post-fee-calculation-404.json");

      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(SUBMISSION_ID);
      var validationMessagePatches =
          submissionValidationContext.getClaimReport(CLAIM_ID).get().getMessages();
      var filteredMessagePatch =
          validationMessagePatches.stream()
              .filter(
                  validationMessagePatch ->
                      validationMessagePatch
                          .getDisplayMessage()
                          .equals(
                              ClaimValidationError.INVALID_FEE_CALCULATION_VALIDATION_FAILED
                                  .getDisplayMessage()))
              .toList();

      Assertions.assertFalse(filteredMessagePatch.isEmpty());
    }
  }

  @Nested
  @DisplayName("Claim level validation tests")
  class ClaimLevelValidationTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource("crimeLowerClaimScenarios")
    void shouldValidateCrimeLowerClaimsCorrectly(
        String displayName, String claimsJson, Consumer<SubmissionValidationContext> assertion)
        throws Exception {

      // Given
      stubForGetSubmission(
          SUBMISSION_ID, "data-claims/get-submission/get-submission-with-claim-crime-lower.json");
      stubForUpdateSubmission(SUBMISSION_ID);

      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(Parameter.param("requireOpenStatus", "false")),
          "provider-details/incident-crime.json");

      stubForGetClaims(Collections.emptyList(), claimsJson);
      stubForGetFeeDetails("CAPA", "fee-scheme/get-fee-details-200.json");
      stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");
      stubForUpdateClaim(SUBMISSION_ID, UUID.fromString(CLAIM_ID));
      stubForUpdateSubmission(SUBMISSION_ID);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param(OFFICES_PARAM, OFFICE_CODE),
              Parameter.param(AREA_OF_LAW_PARAM, AreaOfLaw.CRIME_LOWER.name()),
              Parameter.param(SUBMISSION_PERIOD_PARAM, "APR-2025")),
          SUBMISSIONS_BY_FILTER_NO_CONTENT_JSON);

      // When
      SubmissionValidationContext context =
          submissionValidationService.validateSubmission(SUBMISSION_ID);

      // Then
      assertion.accept(context);
    }

    static Stream<Arguments> crimeLowerClaimScenarios() {
      return Stream.of(
          Arguments.of(
              "Should have no errors when the claim has valid values",
              "data-claims/get-claims/claim-valid.json",
              (Consumer<SubmissionValidationContext>) ContextUtil::assertContextHasNoErrors),
          Arguments.of(
              "Should have no errors when the claim has valid values and includes a valid fee calculation response",
              "data-claims/get-claims/claim-valid-with-fee-calculation.json",
              (Consumer<SubmissionValidationContext>) ContextUtil::assertContextHasNoErrors),
          Arguments.of(
              "Should have no errors when the claim has valid values and includes a null fee calculation response",
              "data-claims/get-claims/claim-valid-with-fee-calculation-null.json",
              (Consumer<SubmissionValidationContext>) ContextUtil::assertContextHasNoErrors),
          Arguments.of(
              "Should have no errors when the claim has valid values and includes a empty fee calculation response",
              "data-claims/get-claims/claim-valid-with-fee-calculation-empty.json",
              (Consumer<SubmissionValidationContext>) ContextUtil::assertContextHasNoErrors),
          Arguments.of(
              "Should have an error when the crime matter type code is invalid",
              "data-claims/get-claims/claim-invalid-crime-matter-type-code.json",
              (Consumer<SubmissionValidationContext>)
                  context -> {
                    ValidationMessagePatch expected =
                        new ValidationMessagePatch()
                            .type(ValidationMessageType.ERROR)
                            .source("Data-Claims-Event-Service")
                            .displayMessage(
                                "Crime Lower Matter Type Code must be one of the permitted values. Please refer to the guidance.")
                            .technicalMessage(
                                "crime_matter_type_code: does not match the regex pattern ^(?:$|0[1-9]|[1-9]|1[0-3]|1[56]|1[89]|2[13]|3[3-8])$ (provided value: 99)");

                    ValidationMessagePatch actual =
                        context.getClaimReports().getFirst().getMessages().getFirst();

                    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
                  }));
    }
  }

  @Nested
  @DisplayName("Incident-shaped end-to-end submission tests")
  class IncidentShapeEndToEndTests {

    @Test
    @DisplayName(
        "Forward-order incident: later crime-dated claims should be authorised (no INVALID_CATEGORY errors)")
    void incidentShapeForwardOrderShouldAuthoriseLaterCrimeClaims() throws Exception {
      // Given: multi-claim submission (5 early CIVIL dated, 3 later CRIME dated)
      stubForGetSubmission(
          SUBMISSION_ID, "data-claims/get-submission/get-submission-incident-multi.json");
      stubForUpdateSubmission(SUBMISSION_ID);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      // Ensure PATCH updates for each claim are stubbed (no-op)
      for (String claimId :
          List.of(
              "11111111-1111-1111-1111-111111111111",
              "22222222-2222-2222-2222-222222222222",
              "33333333-3333-3333-3333-333333333333",
              "44444444-4444-4444-4444-444444444444",
              "55555555-5555-5555-5555-555555555555",
              "66666666-6666-6666-6666-666666666666",
              "77777777-7777-7777-7777-777777777777",
              "88888888-8888-8888-8888-888888888888")) {
        stubForUpdateClaim(SUBMISSION_ID, UUID.fromString(claimId));
      }

      // Return claims in the forward order
      stubForGetClaims(
          Collections.emptyList(), "data-claims/get-claims/incident-multi-forward.json");

      // Provider details: for Civil effective date return an open-ended Civil schedule
      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(
              Parameter.param("effectiveDate", "01-06-2025"),
              Parameter.param("requireOpenStatus", "false")),
          "provider-details/incident-civil-bounded.json");

      // For the later Crime date return a Crime schedule
      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(
              Parameter.param("effectiveDate", "01-07-2026"),
              Parameter.param("requireOpenStatus", "false")),
          "provider-details/incident-crime.json");

      // Fee details and calculation
      stubForGetFeeDetails("CAPA", "fee-scheme/get-fee-details-200.json");
      stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");

      // Avoid duplicate-submission check interference
      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param("offices", OFFICE_CODE),
              Parameter.param("area_of_law", AreaOfLaw.LEGAL_HELP.name()),
              Parameter.param("submission_period", "APR-2025")),
          "data-claims/get-submission/get-submissions-by-filter_no_content.json");

      // When
      SubmissionValidationContext context =
          submissionValidationService.validateSubmission(SUBMISSION_ID);

      // Then: later crime claims (c6..c8) must not be flagged for INVALID_CATEGORY
      for (String claimId :
          List.of(
              "66666666-6666-6666-6666-666666666666",
              "77777777-7777-7777-7777-777777777777",
              "88888888-8888-8888-8888-888888888888")) {
        var report = context.getClaimReport(claimId);
        Assertions.assertTrue(report.isPresent());
        if (report.get().hasErrors()) {
          Assertions.fail(
              "Expected no claim-level errors for "
                  + claimId
                  + " messages="
                  + report.get().getMessages());
        }
      }
    }

    @Test
    @DisplayName(
        "Reverse-order incident: per-claim verdicts should match forward order (no errors for later crime claims)")
    void incidentShapeReverseOrderShouldProduceSamePerClaimVerdicts() throws Exception {
      // Given: same submission but claims returned in reverse (crime first)
      stubForGetSubmission(
          SUBMISSION_ID, "data-claims/get-submission/get-submission-incident-multi.json");
      stubForUpdateSubmission(SUBMISSION_ID);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      for (String claimId :
          List.of(
              "11111111-1111-1111-1111-111111111111",
              "22222222-2222-2222-2222-222222222222",
              "33333333-3333-3333-3333-333333333333",
              "44444444-4444-4444-4444-444444444444",
              "55555555-5555-5555-5555-555555555555",
              "66666666-6666-6666-6666-666666666666",
              "77777777-7777-7777-7777-777777777777",
              "88888888-8888-8888-8888-888888888888")) {
        stubForUpdateClaim(SUBMISSION_ID, UUID.fromString(claimId));
      }

      stubForGetClaims(
          Collections.emptyList(), "data-claims/get-claims/incident-multi-reverse.json");

      // Same provider stubs as forward case
      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(
              Parameter.param("effectiveDate", "01-06-2025"),
              Parameter.param("requireOpenStatus", "false")),
          "provider-details/incident-civil-bounded.json");
      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(
              Parameter.param("effectiveDate", "01-07-2026"),
              Parameter.param("requireOpenStatus", "false")),
          "provider-details/incident-crime.json");

      stubForGetFeeDetails("CAPA", "fee-scheme/get-fee-details-200.json");
      stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param("offices", OFFICE_CODE),
              Parameter.param("area_of_law", AreaOfLaw.LEGAL_HELP.name()),
              Parameter.param("submission_period", "APR-2025")),
          "data-claims/get-submission/get-submissions-by-filter_no_content.json");

      // When
      SubmissionValidationContext context =
          submissionValidationService.validateSubmission(SUBMISSION_ID);

      // Then: per-claim verdicts for crime claims must match the forward-order test (i.e. no
      // errors)
      for (String claimId :
          List.of(
              "66666666-6666-6666-6666-666666666666",
              "77777777-7777-7777-7777-777777777777",
              "88888888-8888-8888-8888-888888888888")) {
        var report = context.getClaimReport(claimId);
        Assertions.assertTrue(report.isPresent());
        Assertions.assertFalse(
            report.get().hasErrors(), "Expected no claim-level errors for " + claimId);
      }
    }

    @Test
    @DisplayName(
        "Single-window baseline: when all claims fall in the same crime window, all claims should pass")
    void singleWindowBaselineShouldStillPassAllClaims() throws Exception {
      // Build a submission where every claim falls into the same Crime window (all crime dated)
      stubForGetSubmission(
          SUBMISSION_ID, "data-claims/get-submission/get-submission-incident-multi.json");
      stubForUpdateSubmission(SUBMISSION_ID);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      for (String claimId :
          List.of(
              "11111111-1111-1111-1111-111111111111",
              "22222222-2222-2222-2222-222222222222",
              "33333333-3333-3333-3333-333333333333",
              "44444444-4444-4444-4444-444444444444",
              "55555555-5555-5555-5555-555555555555",
              "66666666-6666-6666-6666-666666666666",
              "77777777-7777-7777-7777-777777777777",
              "88888888-8888-8888-8888-888888888888")) {
        stubForUpdateClaim(SUBMISSION_ID, UUID.fromString(claimId));
      }

      // Return claims all with crime dates by reusing the forward fixture but we will stub provider
      // to always return Crime schedule for the case date
      stubForGetClaims(
          Collections.emptyList(), "data-claims/get-claims/incident-multi-forward.json");

      // For any effective date used in the fixture, return the Crime schedule
      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(
              Parameter.param("effectiveDate", "01-06-2025"),
              Parameter.param("requireOpenStatus", "false")),
          "provider-details/incident-crime.json");
      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(
              Parameter.param("effectiveDate", "01-07-2026"),
              Parameter.param("requireOpenStatus", "false")),
          "provider-details/incident-crime.json");

      stubForGetFeeDetails("CAPA", "fee-scheme/get-fee-details-200.json");
      stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param("offices", OFFICE_CODE),
              Parameter.param("area_of_law", AreaOfLaw.LEGAL_HELP.name()),
              Parameter.param("submission_period", "APR-2025")),
          "data-claims/get-submission/get-submissions-by-filter_no_content.json");

      // When
      SubmissionValidationContext context =
          submissionValidationService.validateSubmission(SUBMISSION_ID);

      // Then: expect no claim-level errors for any claim
      // ...existing code...
      for (String claimId :
          List.of(
              "11111111-1111-1111-1111-111111111111",
              "22222222-2222-2222-2222-222222222222",
              "33333333-3333-3333-3333-333333333333",
              "44444444-4444-4444-4444-444444444444",
              "55555555-5555-5555-5555-555555555555",
              "66666666-6666-6666-6666-666666666666",
              "77777777-7777-7777-7777-777777777777",
              "88888888-8888-8888-8888-888888888888")) {
        var report = context.getClaimReport(claimId);
        Assertions.assertTrue(report.isPresent());
        Assertions.assertFalse(
            report.get().hasErrors(), "Expected no claim-level errors for " + claimId);
      }
    }

    @Test
    @DisplayName(
        "Genuine unauthorised claim: crime-dated claim should be flagged INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER")
    void genuineUnauthorisedClaimShouldStillBeFlagged() throws Exception {
      // Given a single claim whose date maps to a provider schedule that lacks the required
      // category
      stubForGetSubmission(
          SUBMISSION_ID, "data-claims/get-submission/get-submission-incident-multi.json");
      stubForUpdateSubmission(SUBMISSION_ID);
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      // Ensure PATCH updates for each claim are stubbed (no-op) so MockServer doesn't return 502
      for (String claimId :
          List.of(
              "11111111-1111-1111-1111-111111111111",
              "22222222-2222-2222-2222-222222222222",
              "33333333-3333-3333-3333-333333333333",
              "44444444-4444-4444-4444-444444444444",
              "55555555-5555-5555-5555-555555555555",
              "66666666-6666-6666-6666-666666666666",
              "77777777-7777-7777-7777-777777777777",
              "88888888-8888-8888-8888-888888888888")) {
        stubForUpdateClaim(SUBMISSION_ID, UUID.fromString(claimId));
      }
      // Use the forward claims fixture but stub PDA for the crime date to return CIVIL only
      stubForGetClaims(
          Collections.emptyList(), "data-claims/get-claims/incident-multi-forward.json");

      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(
              Parameter.param("effectiveDate", "01-06-2025"),
              Parameter.param("requireOpenStatus", "false")),
          "provider-details/incident-civil.json");
      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(
              Parameter.param("effectiveDate", "01-07-2026"),
              Parameter.param("requireOpenStatus", "false")),
          "provider-details/incident-civil.json");

      stubForGetFeeDetails("CAPA", "fee-scheme/get-fee-details-200.json");
      stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param("offices", OFFICE_CODE),
              Parameter.param("area_of_law", AreaOfLaw.LEGAL_HELP.name()),
              Parameter.param("submission_period", "APR-2025")),
          "data-claims/get-submission/get-submissions-by-filter_no_content.json");

      // When
      SubmissionValidationContext context =
          submissionValidationService.validateSubmission(SUBMISSION_ID);

      // Then: the crime-dated claim must be flagged as
      // INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER
      Assertions.assertTrue(context.hasErrors("66666666-6666-6666-6666-666666666666"));
    }
  }
}
