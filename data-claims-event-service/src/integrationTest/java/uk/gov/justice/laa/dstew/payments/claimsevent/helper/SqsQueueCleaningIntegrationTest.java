package uk.gov.justice.laa.dstew.payments.claimsevent.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/** Shared base for integration tests that need the LocalStack SQS queue drained between tests. */
public abstract class SqsQueueCleaningIntegrationTest extends MockServerIntegrationTest {

  @Autowired protected SqsClient sqsClient;

  @AfterEach
  void cleanupQueue() {
    String queueUrl =
        sqsClient
            .getQueueUrl(
                GetQueueUrlRequest.builder().queueName(MessageListenerBase.queueName).build())
            .queueUrl();

    await()
        .pollInterval(Duration.ofMillis(200))
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              drainQueue(queueUrl);
              var attributes =
                  sqsClient
                      .getQueueAttributes(
                          GetQueueAttributesRequest.builder()
                              .queueUrl(queueUrl)
                              .attributeNames(
                                  QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                                  QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)
                              .build())
                      .attributes();
              assertThat(attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES))
                  .isEqualTo("0");
              assertThat(
                      attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE))
                  .isEqualTo("0");
            });
  }

  private void drainQueue(String queueUrl) {
    while (true) {
      var messages =
          sqsClient
              .receiveMessage(
                  ReceiveMessageRequest.builder()
                      .queueUrl(queueUrl)
                      .maxNumberOfMessages(10)
                      .visibilityTimeout(0)
                      .waitTimeSeconds(0)
                      .build())
              .messages();
      if (messages.isEmpty()) {
        return;
      }

      messages.forEach(
          message ->
              sqsClient.deleteMessage(
                  DeleteMessageRequest.builder()
                      .queueUrl(queueUrl)
                      .receiptHandle(message.receiptHandle())
                      .build()));
    }
  }
}
