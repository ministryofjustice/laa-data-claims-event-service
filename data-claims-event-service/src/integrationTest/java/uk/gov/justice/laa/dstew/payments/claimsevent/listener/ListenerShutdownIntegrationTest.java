package uk.gov.justice.laa.dstew.payments.claimsevent.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.Lifecycle;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.LocalstackBaseIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.SubmissionEventType;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.BulkParsingService;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SubmissionValidationService;

/**
 * Verifies the SQS listener's graceful shutdown behaviour: an in-flight message that has not
 * finished processing when shutdown is triggered must never be acknowledged, and the shutdown
 * itself must be bounded by the configured listener/acknowledgement shutdown timeouts rather than
 * waiting indefinitely for the still-running handler.
 */
@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@TestPropertySource(
    properties = {
      "laa.bulk-claim-queue.listener-shutdown-timeout-seconds=2",
      "laa.bulk-claim-queue.acknowledgement-shutdown-timeout-seconds=2"
    })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Listener shutdown integration test")
class ListenerShutdownIntegrationTest extends LocalstackBaseIntegrationTest {

  @MockitoBean BulkParsingService bulkParsingService;

  @MockitoBean SubmissionValidationService submissionValidationService;

  @MockitoBean EventServiceMetricService eventServiceMetricService;

  @Autowired MessageListenerContainerRegistry containerRegistry;

  @Test
  @DisplayName("An interrupted message is never acknowledged and the shutdown is bounded")
  void interruptedMessage_isNeverAcknowledged_andShutdownIsBounded() throws Exception {
    UUID bulkSubmissionId = UUID.randomUUID();
    UUID submissionId = UUID.randomUUID();

    CountDownLatch handlerStarted = new CountDownLatch(1);
    // Never counted down: simulates a handler that is still mid-processing when shutdown
    // begins and never completes within the lifetime of this test, so it can never reach the
    // point of acknowledging the message.
    CountDownLatch releaseHandler = new CountDownLatch(1);

    doAnswer(
            _ -> {
              handlerStarted.countDown();
              releaseHandler.await();
              return null;
            })
        .when(bulkParsingService)
        .parseData(any(), any());

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

    // Wait until the handler has actually started processing (message is in-flight).
    assertThat(handlerStarted.await(10, TimeUnit.SECONDS)).isTrue();

    // Simulate shutdown: stop every registered listener container, the same lifecycle hook
    // Spring invokes during graceful application shutdown.
    Instant stopStarted = Instant.now();
    containerRegistry.getListenerContainers().forEach(Lifecycle::stop);
    Duration stopDuration = Duration.between(stopStarted, Instant.now());

    // The container must not block beyond the configured listener + acknowledgement shutdown
    // budget (2s + 2s here), proving shutdown is bounded rather than waiting indefinitely for
    // the still-blocked handler.
    assertThat(stopDuration).isLessThan(Duration.ofSeconds(6));

    // The message must never have been deleted/acknowledged: it must still be present in the
    // queue (either still "in-flight"/not visible, or already redelivered).
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(totalMessageCount()).isEqualTo(1));

    releaseHandler.countDown();
  }

  private int totalMessageCount() {
    var attributes =
        sqsClient
            .getQueueAttributes(
                builder ->
                    builder
                        .queueUrl(queueUrl)
                        .attributeNames(
                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE))
            .attributes();
    return Integer.parseInt(attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES))
        + Integer.parseInt(
            attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE));
  }
}
