package uk.gov.justice.laa.dstew.payments.claimsevent.listener;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.LocalstackBaseIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.BulkParsingService;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Bulk submissions listener integration test")
public class BulkSubmissionListenerIntegrationTests extends LocalstackBaseIntegrationTest {

  @InjectMocks
  BulkSubmissionListener listener;

  @MockitoBean
  BulkParsingService bulkParsingService;

  @Test
  @DisplayName("Should process multiple submissions")
  void shouldProcessMultipleSubmissions() throws JsonProcessingException {
    UUID bulkSubmissionId = new UUID(0, 0);
    UUID submissionIdOne = new UUID(1, 1);
    UUID submissionIdTwo = new UUID(2, 2);

    String messageBody = objectMapper.writeValueAsString(
        Map.of(
            "bulk_submission_id", bulkSubmissionId,
            "submission_ids", List.of(submissionIdOne, submissionIdTwo)
        ));

    // Send message to queue
    sqsClient.sendMessage(
        builder ->
            builder
                .queueUrl(this.queueUrl)
                .messageBody(messageBody));

    // Use await to assert once the listener has received the message from the queue, and passed
    // the submission to the bulkParsingService
    await().pollInterval(Duration.ofMillis(500))
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> {
          verify(bulkParsingService, times(1)).parseData(bulkSubmissionId, submissionIdOne);
          verify(bulkParsingService, times(1)).parseData(bulkSubmissionId, submissionIdTwo);
        });

  }
}
