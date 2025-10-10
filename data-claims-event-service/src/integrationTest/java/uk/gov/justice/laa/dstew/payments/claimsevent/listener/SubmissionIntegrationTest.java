package uk.gov.justice.laa.dstew.payments.claimsevent.listener;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.LocalstackBaseIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.SubmissionEventType;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Bulk submissions listener integration test without mocks")
public class SubmissionIntegrationTest extends LocalstackBaseIntegrationTest {

  @Autowired SubmissionListener listener;

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @Test
  @DisplayName("Should process parse bulk submission event")
  void shouldProcessParseBulkSubmissionEvent() throws JsonProcessingException {
    UUID bulkSubmissionId = new UUID(0, 0);
    UUID submissionIdOne = new UUID(1, 1);
    UUID submissionIdTwo = new UUID(2, 2);

    String messageBody =
        objectMapper.writeValueAsString(
            Map.of(
                "bulk_submission_id",
                bulkSubmissionId,
                "submission_ids",
                List.of(submissionIdOne, submissionIdTwo)));

    // Send message to queue
    sqsClient.sendMessage(
        builder ->
            builder
                .queueUrl(this.queueUrl)
                .messageBody(messageBody)
                .messageAttributes(
                    Map.of(
                        "SubmissionEventType",
                        MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(SubmissionEventType.PARSE_BULK_SUBMISSION.toString())
                            .build())));

    // Use await to assert once the listener has received the message from the queue, and passed
    // the submission to the bulkParsingService
    await()
        .atMost(3, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(
                      dataClaimsRestClient
                          .getSubmission(submissionIdOne)
                          .getBody()
                          .getSubmissionId())
                  .isEqualTo(submissionIdOne);
            });
  }

  //  private Integer numberOfMessagesInQueue() {
  //    GetQueueAttributesResult attributes = SQS.
  //            .getQueueAttributes(consumerQueueName, of("All"));
  //
  //    return Integer.parseInt(
  //            attributes.getAttributes().get("ApproximateNumberOfMessages")
  //    );
  //  }
}
