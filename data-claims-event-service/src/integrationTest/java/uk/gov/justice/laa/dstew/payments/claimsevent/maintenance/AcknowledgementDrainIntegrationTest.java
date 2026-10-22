package uk.gov.justice.laa.dstew.payments.claimsevent.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.LocalstackBaseIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.SubmissionEventType;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.BulkParsingService;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SubmissionValidationService;

/**
 * Verifies that the in-flight count only reaches zero once a message's acknowledgement has
 * actually completed - not merely once its listener method returns - so a preStop/shutdown hook
 * polling the in-flight count can never observe "drained" while acknowledgement of a just-finished
 * message is still being flushed.
 */
@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Acknowledgement drain integration test")
class AcknowledgementDrainIntegrationTest extends LocalstackBaseIntegrationTest {

  @MockitoBean BulkParsingService bulkParsingService;

  @MockitoBean SubmissionValidationService submissionValidationService;

  @MockitoBean EventServiceMetricService eventServiceMetricService;

  @MockitoSpyBean AcknowledgementTrackingCallback acknowledgementTrackingCallback;

  @Autowired ListenerActivityTracker listenerActivityTracker;

  @Test
  @DisplayName(
      "In-flight count stays above zero while acknowledgement is pending, "
          + "and reaches zero once it completes")
  void inFlightCountStaysNonZeroUntilAcknowledgementCompletes() throws Exception {
    // Artificially delay the acknowledgement callback so there is an observable window between
    // "processing finished" and "acknowledgement settled".
    Duration ackDelay = Duration.ofSeconds(3);
    doAnswer(
            invocation -> {
              Thread.sleep(ackDelay.toMillis());
              return invocation.callRealMethod();
            })
        .when(acknowledgementTrackingCallback)
        .onSuccess(anyCollection());

    UUID bulkSubmissionId = UUID.randomUUID();
    UUID submissionId = UUID.randomUUID();

    String messageBody =
        objectMapper.writeValueAsString(
            Map.of("bulk_submission_id", bulkSubmissionId, "submission_ids", List.of(submissionId)));

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

    // Wait until the listener method has actually finished processing the message.
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> verify(bulkParsingService).parseData(bulkSubmissionId, submissionId));

    // Processing has finished, but the (artificially delayed) acknowledgement has not - the
    // message must still be counted as in-flight/unsettled.
    assertThat(listenerActivityTracker.getInFlightCount()).isEqualTo(1);

    // Once the acknowledgement completes, the in-flight count must drop to zero.
    await()
        .atMost(ackDelay.plusSeconds(5))
        .untilAsserted(() -> assertThat(listenerActivityTracker.getInFlightCount()).isZero());
  }
}
