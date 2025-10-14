// package uk.gov.justice.laa.dstew.payments.claimsevent.listener;
//
// import static org.awaitility.Awaitility.await;
// import static org.mockserver.model.HttpRequest.request;
//
// import com.fasterxml.jackson.databind.ObjectMapper;
// import io.awspring.cloud.sqs.operations.SqsTemplate;
// import java.time.Duration;
// import java.util.List;
// import java.util.Map;
// import java.util.UUID;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.TestInstance;
// import org.mockserver.model.Parameter;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.testcontainers.context.ImportTestcontainers;
// import org.springframework.context.annotation.Import;
// import org.springframework.test.context.ActiveProfiles;
// import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
// import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
// import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
// import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
// import uk.gov.justice.laa.dstew.payments.claimsevent.model.SubmissionEventType;
//
// @ActiveProfiles("test")
// @ImportTestcontainers(MessageListenerBase.class)
// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
// @Import(MockServerIntegrationTest.ClaimsConfiguration.class)
// public class MessageListenerIntegrationTest extends MockServerIntegrationTest {
//
//  private static final String OFFICE_CODE = "AQ2B3C";
//  private static final String AREA_OF_LAW = "CIVIL";
//  private static final String API_VERSION_0 = "/api/v0/";
//
//  @Autowired private SqsTemplate sqsTemplate;
//  @Autowired private ObjectMapper objectMapper;
//
//  @Test
//  void sendMessage() throws Exception {
//    UUID submissionId = UUID.fromString("0561d67b-30ed-412e-8231-f6296a53538d");
//    // Given
//    stubForGetSubmission(submissionId, "data-claims/get-submission/get-submission-APR-25.json");
//    SubmissionPatch patchBodyInProgress =
//        SubmissionPatch.builder()
//            .submissionId(submissionId)
//            .status(SubmissionStatus.VALIDATION_IN_PROGRESS)
//            .build();
//    stubForUpdateSubmissionWithBody(submissionId, patchBodyInProgress);
//    SubmissionPatch patchBodySucceeded =
//        SubmissionPatch.builder()
//            .submissionId(submissionId)
//            .status(SubmissionStatus.VALIDATION_SUCCEEDED)
//            .build();
//    stubForUpdateSubmissionWithBody(submissionId, patchBodySucceeded);
//    stubReturnNoClaims(submissionId);
//
//    getStubForGetSubmissionByCriteria(
//        List.of(
//            Parameter.param("offices", OFFICE_CODE),
//            Parameter.param("area-of-law", AREA_OF_LAW),
//            Parameter.param("submission-period", "APR-2025")),
//        "data-claims/get-submission/get-submissions-by-filter_no_content.json");
//
//    String messageBody = objectMapper.writeValueAsString(Map.of("submission_id", submissionId));
//
//    sqsTemplate.send(
//        toQueue ->
//            toQueue
//                .queue("test-queue-name")
//                .payload(messageBody)
//                .header("SubmissionEventType",
// SubmissionEventType.VALIDATE_SUBMISSION.toString()));
//
//    await()
//        .pollInterval(Duration.ofMillis(500))
//        .atMost(Duration.ofSeconds(100))
//        .untilAsserted(
//            () -> {
//              mockServerClient.verify(
//                  request().withPath(API_VERSION_0 + "submissions/" + submissionId));
//              //                            var response =
//              // dataClaimsRestClient.getSubmission(submissionId);
//              //                            var submissionResponse = response.getBody();
//              //
// assertTrue(response.getStatusCode().is2xxSuccessful());
//              //                            assertNotNull(submissionResponse);
//              //
//              //
// assertThat(submissionResponse.getStatus()).isEqualTo(SubmissionStatus.VALIDATION_SUCCEEDED);
//            });
//  }
// }
