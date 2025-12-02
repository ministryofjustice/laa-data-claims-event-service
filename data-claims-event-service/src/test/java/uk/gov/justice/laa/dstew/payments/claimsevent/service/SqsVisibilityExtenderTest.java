package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

@ExtendWith(MockitoExtension.class)
public class SqsVisibilityExtenderTest {

  private static final String QUEUE_NAME = "test-queue";
  private static final int VISIBILITY_TIMEOUT_SECONDS = 60;
  private static final int VISIBILITY_EXTENSION_INTERVAL_SECONDS = 2;
  private static final String QUEUE_URL = "http://test-queue-url";
  private static final String RECEIPT_HANDLE = "test-receipt-handle";

  @Captor
  private ArgumentCaptor<ChangeMessageVisibilityRequest>
      changeMessageVisibilityRequestArgumentCaptor;

  @Captor private ArgumentCaptor<GetQueueUrlRequest> getQueueUrlRequestArgumentCaptor;
  @Mock private SqsClient mockSqsClient;
  private SqsVisibilityExtender sqsVisibilityExtender;

  @BeforeEach
  public void setUp() {
    when(mockSqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenReturn(GetQueueUrlResponse.builder().queueUrl(QUEUE_URL).build());
    sqsVisibilityExtender =
        new SqsVisibilityExtender(
            mockSqsClient,
            QUEUE_NAME,
            VISIBILITY_TIMEOUT_SECONDS,
            VISIBILITY_EXTENSION_INTERVAL_SECONDS);
  }

  @DisplayName("Should call change visibility on SQS when start is called")
  @Test
  public void testStart() throws InterruptedException {

    sqsVisibilityExtender.start(RECEIPT_HANDLE);
    Thread.sleep(Duration.ofSeconds(2).toMillis());

    verify(mockSqsClient, timeout(1000).atLeastOnce())
        .changeMessageVisibility(changeMessageVisibilityRequestArgumentCaptor.capture());
    assertThat(changeMessageVisibilityRequestArgumentCaptor.getValue().receiptHandle())
        .isEqualTo(RECEIPT_HANDLE);
    assertThat(changeMessageVisibilityRequestArgumentCaptor.getValue().queueUrl())
        .isEqualTo(QUEUE_URL);
    assertThat(changeMessageVisibilityRequestArgumentCaptor.getValue().visibilityTimeout())
        .isEqualTo(VISIBILITY_TIMEOUT_SECONDS);
    sqsVisibilityExtender.close();
  }
}
