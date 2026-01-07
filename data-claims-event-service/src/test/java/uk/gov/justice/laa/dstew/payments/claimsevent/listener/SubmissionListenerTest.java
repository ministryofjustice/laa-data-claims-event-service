package uk.gov.justice.laa.dstew.payments.claimsevent.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.SubmissionEventProcessingException;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionMessage;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.SubmissionEventType;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.SubmissionValidationMessage;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.BulkParsingService;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SqsVisibilityExtender;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SubmissionValidationService;

@ExtendWith(MockitoExtension.class)
public class SubmissionListenerTest {

  @Mock BulkParsingService bulkParsingService;

  @Mock SubmissionValidationService submissionValidationService;

  @Mock ObjectMapper objectMapper;

  @Mock SqsVisibilityExtender mockSqsVisibilityExtender;
  @Mock ObjectProvider<SqsVisibilityExtender> mockVisibilityExtenderProvider;

  @InjectMocks SubmissionListener submissionListener;

  @Nested
  @DisplayName("receiveSubmissionEvent")
  class ReceiveSubmissionEventTests {

    @Test
    @DisplayName("Handles parse bulk submission events")
    void handlesBulkSubmissionEvent() throws JsonProcessingException {
      // Given
      Message message =
          Message.builder()
              .body("body")
              .receiptHandle("receiptHandle")
              .messageAttributes(
                  Map.of(
                      "SubmissionEventType",
                      MessageAttributeValue.builder()
                          .stringValue(SubmissionEventType.PARSE_BULK_SUBMISSION.toString())
                          .build()))
              .build();

      UUID bulkSubmissionId = new UUID(0, 0);
      UUID submissionId1 = new UUID(1, 1);
      UUID submissionId2 = new UUID(2, 2);

      BulkSubmissionMessage bulkSubmissionMessage =
          new BulkSubmissionMessage(bulkSubmissionId, List.of(submissionId1, submissionId2));

      when(objectMapper.readValue("body", BulkSubmissionMessage.class))
          .thenReturn(bulkSubmissionMessage);

      when(mockVisibilityExtenderProvider.getObject()).thenReturn(mockSqsVisibilityExtender);
      doNothing().when(mockSqsVisibilityExtender).start("receiptHandle");
      // When
      submissionListener.receiveSubmissionEvent(message);

      // Then
      verify(bulkParsingService).parseData(bulkSubmissionId, submissionId1);
      verify(bulkParsingService).parseData(bulkSubmissionId, submissionId2);
      verifyNoMoreInteractions(bulkParsingService);

      verifyNoInteractions(submissionValidationService);
      verify(mockSqsVisibilityExtender).start("receiptHandle");
    }

    @Test
    @DisplayName("Handles submission validation events")
    void handlesSubmissionValidationEvent() throws JsonProcessingException {
      // Given
      Message message =
          Message.builder()
              .body("body")
              .receiptHandle("receiptHandle")
              .messageAttributes(
                  Map.of(
                      "SubmissionEventType",
                      MessageAttributeValue.builder()
                          .stringValue(SubmissionEventType.VALIDATE_SUBMISSION.toString())
                          .build()))
              .build();

      UUID submissionId = new UUID(0, 0);

      SubmissionValidationMessage submissionValidationMessage =
          new SubmissionValidationMessage(submissionId);

      when(objectMapper.readValue("body", SubmissionValidationMessage.class))
          .thenReturn(submissionValidationMessage);

      when(mockVisibilityExtenderProvider.getObject()).thenReturn(mockSqsVisibilityExtender);
      doNothing().when(mockSqsVisibilityExtender).start("receiptHandle");

      // When
      submissionListener.receiveSubmissionEvent(message);

      // Then
      verify(submissionValidationService).validateSubmission(submissionId);
      verifyNoMoreInteractions(submissionValidationService);

      verifyNoInteractions(bulkParsingService);
      verify(mockSqsVisibilityExtender).start("receiptHandle");
    }

    @Test
    @DisplayName("Handles missing submission event type")
    void handlesMissingSubmissionEventType() {
      // Given
      Message message = Message.builder().body("body").receiptHandle("receiptHandle").build();
      when(mockVisibilityExtenderProvider.getObject()).thenReturn(mockSqsVisibilityExtender);
      doNothing().when(mockSqsVisibilityExtender).start("receiptHandle");

      // When
      ThrowingCallable result = () -> submissionListener.receiveSubmissionEvent(message);

      // Then
      assertThatThrownBy(result)
          .isInstanceOf(SubmissionEventProcessingException.class)
          .hasMessage("Submission event type is missing");
    }

    @Test
    @DisplayName("Handles null submission event type")
    void handlesNullSubmissionEventType() {
      // Given
      Message message =
          Message.builder()
              .body("body")
              .receiptHandle("receiptHandle")
              .messageAttributes(
                  Map.of(
                      "SubmissionEventType",
                      MessageAttributeValue.builder().stringValue(null).build()))
              .build();
      when(mockVisibilityExtenderProvider.getObject()).thenReturn(mockSqsVisibilityExtender);
      doNothing().when(mockSqsVisibilityExtender).start("receiptHandle");

      // When
      ThrowingCallable result = () -> submissionListener.receiveSubmissionEvent(message);

      // Then
      assertThatThrownBy(result)
          .isInstanceOf(SubmissionEventProcessingException.class)
          .hasMessage("Submission event type is missing");
    }

    @Test
    @DisplayName("Handles invalid submission event type")
    void handlesInvalidSubmissionEventType() {
      // Given
      Message message =
          Message.builder()
              .body("body")
              .receiptHandle("receiptHandle")
              .messageAttributes(
                  Map.of(
                      "SubmissionEventType",
                      MessageAttributeValue.builder().stringValue("INVALID_EVENT_TYPE").build()))
              .build();
      when(mockVisibilityExtenderProvider.getObject()).thenReturn(mockSqsVisibilityExtender);
      doNothing().when(mockSqsVisibilityExtender).start("receiptHandle");
      // When
      ThrowingCallable result = () -> submissionListener.receiveSubmissionEvent(message);

      // Then
      assertThatThrownBy(result).isInstanceOf(IllegalArgumentException.class);
    }
  }
}
