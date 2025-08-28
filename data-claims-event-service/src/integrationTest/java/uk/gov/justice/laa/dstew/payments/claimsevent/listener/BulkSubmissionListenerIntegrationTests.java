package uk.gov.justice.laa.dstew.payments.claimsevent.listener;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
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

  public static List<UUID> processedBulkSubmissions = new ArrayList<>();

  // Not actually asserting during test, rather it's dependencies are being stubbed and checked
  //  for invocation.
  @Autowired
  BulkSubmissionListener listener;

  // Define a nested static class for test-specific bean configuration
  @TestConfiguration
  static class StubBulkParsingServiceConfiguration {
    @Bean(name = "bulkParsingService")
    public BulkParsingService bulkParsingService() {
      return new BulkParsingService(null, null) {

        // Override parse data method, and add UUIDs to list of "processed" submissions instead of
        // calling default data as this test is not testing BulkParsingService.
        @Override
        public void parseData(UUID bulkSubmissionId, UUID submissionId) {
          processedBulkSubmissions.add(submissionId);
        }
      };
    }
  }


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

    // Wait until the bulk submission starts being processed
    await().pollInterval(Duration.ofMillis(100))
        .atMost(Duration.ofSeconds(10))
        .until(() -> processedBulkSubmissions.size() == 2);

    assertThat(processedBulkSubmissions.contains(submissionIdOne)).isTrue();
    assertThat(processedBulkSubmissions.contains(submissionIdTwo)).isTrue();
  }
}
