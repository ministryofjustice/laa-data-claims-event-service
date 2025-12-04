package uk.gov.justice.laa.dstew.payments.claimsevent.listener;

import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockserver.model.Parameter;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.SubmissionEventType;

@ActiveProfiles("test")
@ImportTestcontainers(MessageListenerBase.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(MockServerIntegrationTest.ClaimsConfiguration.class)
public class MessageListenerIntegrationTest extends MockServerIntegrationTest {

  private static final String OFFICE_CODE = "AQ2B3C";
  private static final AreaOfLaw AREA_OF_LAW = AreaOfLaw.LEGAL_HELP;
  private static final String API_VERSION_0 = "/api/v0/";
  private static final UUID SUBMISSION_ID = UUID.fromString("0561d67b-30ed-412e-8231-f6296a53538d");
  private static final UUID BULK_SUBMISSION_ID =
      UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
  private static final UUID CLAIM_ID = UUID.fromString("f6bde766-a0a3-483b-bf13-bef888b4f06e");

  @Autowired private SqsTemplate sqsTemplate;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void sendMessage_noErrors_noClaims() throws Exception {
    // Given a submission with no claims mocked
    stubForGetSubmission(SUBMISSION_ID, "data-claims/get-submission/get-submission-APR-25.json");
    SubmissionPatch patchBodyInProgress =
        SubmissionPatch.builder()
            .submissionId(SUBMISSION_ID)
            .status(SubmissionStatus.VALIDATION_IN_PROGRESS)
            .build();
    stubForUpdateSubmissionWithBody(SUBMISSION_ID, patchBodyInProgress);
    SubmissionPatch patchBodySucceeded =
        SubmissionPatch.builder()
            .submissionId(SUBMISSION_ID)
            .status(SubmissionStatus.VALIDATION_SUCCEEDED)
            .build();
    stubForUpdateSubmissionWithBody(SUBMISSION_ID, patchBodySucceeded);
    stubReturnNoClaims();

    getStubForGetSubmissionByCriteria(
        List.of(
            Parameter.param("offices", OFFICE_CODE),
            Parameter.param("area_of_law", AREA_OF_LAW.name()),
            Parameter.param("submission_period", "APR-2025")),
        "data-claims/get-submission/get-submissions-by-filter_no_content.json");
    stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);
    // when
    sendSubmissionValidationMessage();

    // then
    verifySubmissionRequests();
  }

  @Test
  void sendMessage_noErrors_withClaims() throws Exception {
    // Given a submission with a claim
    stubForGetSubmission(
        SUBMISSION_ID, "data-claims/get-submission/get-submission-with-claim.json");
    SubmissionPatch patchBodyInProgress =
        SubmissionPatch.builder()
            .submissionId(SUBMISSION_ID)
            .status(SubmissionStatus.VALIDATION_IN_PROGRESS)
            .build();
    stubForUpdateSubmissionWithBody(SUBMISSION_ID, patchBodyInProgress);
    SubmissionPatch patchBodySucceeded =
        SubmissionPatch.builder()
            .submissionId(SUBMISSION_ID)
            .status(SubmissionStatus.VALIDATION_SUCCEEDED)
            .build();
    stubForUpdateSubmissionWithBody(SUBMISSION_ID, patchBodySucceeded);

    getStubForGetSubmissionByCriteria(
        List.of(
            Parameter.param("offices", OFFICE_CODE),
            Parameter.param("area_of_law", AREA_OF_LAW.name()),
            Parameter.param("submission_period", "APR-2025")),
        "data-claims/get-submission/get-submissions-by-filter_no_content.json");
    stubForGetFeeDetails("CAPA", "fee-scheme/get-fee-details-200.json");
    stubForGetProviderOffice(
        OFFICE_CODE,
        List.of(new Parameter("areaOfLaw", AREA_OF_LAW.getValue())),
        "provider-details/get-firm-schedules-openapi-200.json");

    stubForGetClaims(Collections.emptyList(), "data-claims/get-claims/claim-two-claims.json");
    // fee-calculation
    stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");
    // Stub patch claim
    stubForUpdateClaim(SUBMISSION_ID, CLAIM_ID);
    // Stub patch submission
    stubForUpdateSubmission(SUBMISSION_ID);
    // Stub patch bulk submission
    stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

    // when
    sendSubmissionValidationMessage();

    // then
    verifySubmissionAndClaimRequests();
  }

  @Test
  void sendMessage_returnsNoValidationError_withDisbursementClaim_caseStartDateMoreThan3MonthsOld()
      throws Exception {
    // Given a submission with a claim
    stubForGetSubmission(
        SUBMISSION_ID, "data-claims/get-submission/get-submission-with-claim.json");
    SubmissionPatch patchBodyInProgress =
        SubmissionPatch.builder()
            .submissionId(SUBMISSION_ID)
            .status(SubmissionStatus.VALIDATION_IN_PROGRESS)
            .build();
    stubForUpdateSubmissionWithBody(SUBMISSION_ID, patchBodyInProgress);
    SubmissionPatch patchBodySucceeded =
        SubmissionPatch.builder()
            .submissionId(SUBMISSION_ID)
            .status(SubmissionStatus.VALIDATION_SUCCEEDED)
            .build();
    stubForUpdateSubmissionWithBody(SUBMISSION_ID, patchBodySucceeded);

    getStubForGetSubmissionByCriteria(
        List.of(
            Parameter.param("offices", OFFICE_CODE),
            Parameter.param("area_of_law", AREA_OF_LAW.name()),
            Parameter.param("submission_period", "APR-2025")),
        "data-claims/get-submission/get-submissions-by-filter_no_content.json");
    stubForGetFeeDetails("CAPA", "fee-scheme/get-fee-details-disbursement.json");
    stubForGetProviderOffice(
        OFFICE_CODE,
        List.of(new Parameter("areaOfLaw", AREA_OF_LAW.getValue())),
        "provider-details/get-firm-schedules-openapi-200.json");

    // this returns the caseStartDate as 2025-01-01 which is more than 3 months old for the given
    // submission period: APR-2025 (end date: 30-APR-2025)
    stubForGetClaims(
        Collections.emptyList(), "data-claims/get-claims/claim-disbursement-claims.json");
    // fee-calculation
    stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");
    // Stub patch claim
    stubForUpdateClaim(SUBMISSION_ID, CLAIM_ID);
    // Stub patch submission
    stubForUpdateSubmission(SUBMISSION_ID);
    // Stub patch bulk submission
    stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

    // when
    sendSubmissionValidationMessage();

    // then
    verifySubmissionAndClaimRequests();
  }

  @Test
  void sendMessage_returnsValidationError_withDisbursementClaim_caseStartDateLessThan3MonthsOld()
      throws Exception {
    // Given a submission with a claim
    stubForGetSubmission(
        SUBMISSION_ID, "data-claims/get-submission/get-submission-with-claim.json");
    SubmissionPatch patchBodyInProgress =
        SubmissionPatch.builder()
            .submissionId(SUBMISSION_ID)
            .status(SubmissionStatus.VALIDATION_IN_PROGRESS)
            .build();
    stubForUpdateSubmissionWithBody(SUBMISSION_ID, patchBodyInProgress);
    SubmissionPatch patchBodySucceeded =
        SubmissionPatch.builder()
            .submissionId(SUBMISSION_ID)
            .status(SubmissionStatus.VALIDATION_SUCCEEDED)
            .build();
    stubForUpdateSubmissionWithBody(SUBMISSION_ID, patchBodySucceeded);

    getStubForGetSubmissionByCriteria(
        List.of(
            Parameter.param("offices", OFFICE_CODE),
            Parameter.param("area_of_law", AREA_OF_LAW.name()),
            Parameter.param("submission_period", "APR-2025")),
        "data-claims/get-submission/get-submissions-by-filter_no_content.json");
    stubForGetFeeDetails("CAPA", "fee-scheme/get-fee-details-disbursement.json");
    stubForGetProviderOffice(
        OFFICE_CODE,
        List.of(new Parameter("areaOfLaw", AREA_OF_LAW.getValue())),
        "provider-details/get-firm-schedules-openapi-200.json");

    // this returns the caseStartDate as 2025-03-01 which is less than 3 months old for the given
    // submission period: APR-2025 (end date: 30-APR-2025)
    stubForGetClaims(
        Collections.emptyList(),
        "data-claims/get-claims/claim-disbursement-within-3-month-claims.json");
    // fee-calculation
    stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");
    // Stub patch claim
    stubForUpdateClaim(SUBMISSION_ID, CLAIM_ID);
    // Stub patch submission
    stubForUpdateSubmission(SUBMISSION_ID);
    // Stub patch bulk submission
    stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

    // when
    sendSubmissionValidationMessage();

    // then
    verifySubmissionRequests();
    verifyClaimRequestInvocationWithValidationErrorMessage();
  }

  @Test
  void sendMessage_validationFailedDuplicate() throws Exception {
    // Given Validation failed for submission
    stubForGetSubmission(SUBMISSION_ID, "data-claims/get-submission/get-submission-APR-25.json");
    SubmissionPatch patchBodyInProgress =
        SubmissionPatch.builder()
            .submissionId(SUBMISSION_ID)
            .status(SubmissionStatus.VALIDATION_IN_PROGRESS)
            .build();
    stubForUpdateSubmissionWithBody(SUBMISSION_ID, patchBodyInProgress);
    SubmissionPatch patchBodySucceeded =
        SubmissionPatch.builder()
            .submissionId(SUBMISSION_ID)
            .status(SubmissionStatus.VALIDATION_FAILED)
            .build();
    stubForUpdateSubmissionWithBody(SUBMISSION_ID, patchBodySucceeded);
    stubReturnNoClaims();

    getStubForGetSubmissionByCriteria(
        List.of(
            Parameter.param("offices", OFFICE_CODE),
            Parameter.param("area_of_law", AREA_OF_LAW.name()),
            Parameter.param("submission_period", "APR-2025")),
        "data-claims/get-submission/get-submissions-by-filter.json");
    stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);
    // when
    sendSubmissionValidationMessage();

    // then
    verifySubmissionRequests();
  }

  private void sendSubmissionValidationMessage() throws JsonProcessingException {
    String messageBody = objectMapper.writeValueAsString(Map.of("submission_id", SUBMISSION_ID));
    sqsTemplate.send(
        toQueue ->
            toQueue
                .queue("test-queue-name")
                .payload(messageBody)
                .header("SubmissionEventType", SubmissionEventType.VALIDATE_SUBMISSION.toString()));
  }

  private void verifySubmissionRequests() {
    await()
        .pollInterval(Duration.ofMillis(500))
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(this::verifySubmissionRequestInvocation);
  }

  private void verifySubmissionAndClaimRequests() {
    await()
        .pollInterval(Duration.ofMillis(500))
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () -> {
              verifySubmissionRequestInvocation();
              verifyClaimRequestInvocation();
            });
  }

  private void verifySubmissionRequestInvocation() {
    mockServerClient.verify(
        request().withMethod("GET").withPath(API_VERSION_0 + "submissions/" + SUBMISSION_ID));
    mockServerClient.verify(request().withMethod("GET").withPath(API_VERSION_0 + "submissions"));
    mockServerClient.verify(
        request().withMethod("PATCH").withPath(API_VERSION_0 + "submissions/" + SUBMISSION_ID),
        VerificationTimes.exactly(2));
  }

  private void verifyClaimRequestInvocation() throws JsonProcessingException {
    ClaimPatch validClaimPatch =
        ClaimPatch.builder().id(CLAIM_ID.toString()).status(ClaimStatus.VALID).build();
    ClaimPatch feeCalculationPatch =
        ClaimPatch.builder()
            .id(CLAIM_ID.toString())
            .feeCalculationResponse(FeeCalculationPatch.builder().claimId(CLAIM_ID).build())
            .build();
    mockServerClient.verify(
        request()
            .withMethod("PATCH")
            .withPath(API_VERSION_0 + "submissions/" + SUBMISSION_ID + "/claims/" + CLAIM_ID)
            .withBody(json(objectMapper.writeValueAsString(feeCalculationPatch))),
        VerificationTimes.exactly(1));
    mockServerClient.verify(
        request()
            .withMethod("PATCH")
            .withPath(API_VERSION_0 + "submissions/" + SUBMISSION_ID + "/claims/" + CLAIM_ID)
            .withBody(json(objectMapper.writeValueAsString(validClaimPatch))),
        VerificationTimes.exactly(1));
  }

  private void verifyClaimRequestInvocationWithValidationErrorMessage()
      throws JsonProcessingException {
    ClaimPatch invalidClaimPatch =
        ClaimPatch.builder()
            .id(CLAIM_ID.toString())
            .status(ClaimStatus.INVALID)
            .validationMessages(
                List.of(
                    ValidationMessagePatch.builder()
                        .displayMessage(
                            "Disbursement claims can only be submitted at least 3 calendar months after the Case Start Date 01/03/2025")
                        .build()))
            .build();

    mockServerClient.verify(
        request()
            .withMethod("PATCH")
            .withPath(API_VERSION_0 + "submissions/" + SUBMISSION_ID + "/claims/" + CLAIM_ID)
            .withBody(json(objectMapper.writeValueAsString(invalidClaimPatch))),
        VerificationTimes.exactly(1));
  }
}
