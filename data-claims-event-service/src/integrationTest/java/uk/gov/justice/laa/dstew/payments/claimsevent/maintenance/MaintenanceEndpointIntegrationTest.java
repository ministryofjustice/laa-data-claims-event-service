package uk.gov.justice.laa.dstew.payments.claimsevent.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.LocalstackBaseIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.SubmissionEventType;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.BulkParsingService;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SubmissionValidationService;

/**
 * Verifies the full HTTP-driven preStop flow: {@code POST /actuator/maintenance} stops the SQS
 * listener(s) without waiting for in-flight work, {@code GET /actuator/maintenance} reports
 * polling has stopped and the in-flight count draining to zero as the handler completes, and
 * {@code DELETE /actuator/maintenance} resumes polling afterwards.
 */
@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      "laa.bulk-claim-queue.listener-shutdown-timeout-seconds=2",
      "laa.bulk-claim-queue.acknowledgement-shutdown-timeout-seconds=2"
    })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Maintenance endpoint integration test")
class MaintenanceEndpointIntegrationTest extends LocalstackBaseIntegrationTest {

  @MockitoBean BulkParsingService bulkParsingService;

  @MockitoBean SubmissionValidationService submissionValidationService;

  @MockitoBean EventServiceMetricService eventServiceMetricService;

  @LocalManagementPort int managementPort;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Test
  @DisplayName(
      "Pause stops polling without waiting for in-flight work, "
          + "status reports the drain to zero, and resume restarts polling")
  void pauseDrainsThenResumeRestartsPolling() throws Exception {
    UUID bulkSubmissionId = UUID.randomUUID();
    UUID submissionId = UUID.randomUUID();

    CountDownLatch handlerStarted = new CountDownLatch(1);
    CountDownLatch releaseHandler = new CountDownLatch(1);

    doAnswer(
            _ -> {
              handlerStarted.countDown();
              releaseHandler.await();
              return null;
            })
        .when(bulkParsingService)
        .parseData(any(), any());

    sendMessage(bulkSubmissionId, submissionId);

    // Wait until the handler has actually started processing (message is in-flight).
    assertThat(handlerStarted.await(10, TimeUnit.SECONDS)).isTrue();

    // Pause via the real HTTP endpoint: must return immediately (not wait for drain) and report
    // the in-flight message that is still being handled.
    HttpResponse<String> pauseResponse = sendRequest("POST");
    assertThat(pauseResponse.statusCode()).isEqualTo(200);
    assertThat(inFlightCount(pauseResponse.body())).isEqualTo(1);

    // Polling must actually stop (the container-level stop runs asynchronously off the request
    // thread), and the in-flight handler must still be counted until it completes.
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(listenersRunning()).isFalse());
    assertThat(inFlightCount()).isEqualTo(1);

    // Let the handler finish; the in-flight count must then drain to zero.
    releaseHandler.countDown();
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(inFlightCount()).isEqualTo(0));

    // Resume: listeners must start polling again.
    HttpResponse<String> resumeResponse = sendRequest("DELETE");
    assertThat(resumeResponse.statusCode()).isEqualTo(200);
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(listenersRunning()).isTrue());

    // A new message sent after resume must be picked up again, proving polling genuinely
    // restarted rather than merely reporting a stale "running" flag.
    doAnswer(_ -> null).when(bulkParsingService).parseData(any(), any());
    UUID secondSubmissionId = UUID.randomUUID();
    sendMessage(bulkSubmissionId, secondSubmissionId);
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(inFlightCount()).isEqualTo(0));
  }

  private void sendMessage(UUID bulkSubmissionId, UUID submissionId) throws Exception {
    String messageBody =
        objectMapper.writeValueAsString(
            Map.of(
                "bulk_submission_id", bulkSubmissionId,
                "submission_ids", List.of(submissionId)));

    sqsClient.sendMessage(
        builder ->
            builder
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .messageAttributes(
                    Map.of(
                        "SubmissionEventType",
                        MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(SubmissionEventType.PARSE_BULK_SUBMISSION.toString())
                            .build())));
  }

  private HttpResponse<String> sendRequest(String method) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(maintenanceUrl()))
            .method(method, BodyPublishers.noBody())
            .build();
    return httpClient.send(request, BodyHandlers.ofString());
  }

  private boolean listenersRunning() throws Exception {
    return sendRequest("GET").body().contains("\"listenersRunning\":true");
  }

  private int inFlightCount() throws Exception {
    return inFlightCount(sendRequest("GET").body());
  }

  private int inFlightCount(String responseBody) {
    return Integer.parseInt(responseBody.replaceAll(".*\"inFlightCount\":(\\d+).*", "$1"));
  }

  private String maintenanceUrl() {
    return "http://localhost:" + managementPort + "/actuator/maintenance";
  }
}
