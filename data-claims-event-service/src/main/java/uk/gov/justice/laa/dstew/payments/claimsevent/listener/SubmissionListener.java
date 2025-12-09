package uk.gov.justice.laa.dstew.payments.claimsevent.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.SubmissionEventProcessingException;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionMessage;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.SubmissionEventType;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.SubmissionValidationMessage;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.BulkParsingService;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SqsVisibilityExtender;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SubmissionValidationService;

/**
 * Listener for bulk submissions from the Data Claims service.
 *
 * <p>Listens to an SQS queue for any new messages. Processes new bulk submissions when they are
 * added to the queue.
 *
 * @author Jamie Briggs
 * @see BulkSubmissionMessage
 * @see BulkParsingService
 */
@Slf4j
@Component
public class SubmissionListener {

  private final BulkParsingService bulkParsingService;
  private final SubmissionValidationService submissionValidationService;
  private final ObjectMapper objectMapper;
  private final EventServiceMetricService eventServiceMetricService;
  private final ObjectProvider<SqsVisibilityExtender> sqsVisibilityExtenderProvider;

  /**
   * Construct a new {@code SubmissionListener}.
   *
   * @param bulkParsingService the service responsible for parsing bulk submissions
   * @param submissionValidationService the service responsible for validating parsed submissions
   * @param objectMapper object mapper for deserializing event messages
   */
  public SubmissionListener(
      BulkParsingService bulkParsingService,
      SubmissionValidationService submissionValidationService,
      EventServiceMetricService eventServiceMetricService,
      @Qualifier("submissionEventMapper") ObjectMapper objectMapper,
      ObjectProvider<SqsVisibilityExtender> sqsVisibilityExtenderProvider) {
    this.bulkParsingService = bulkParsingService;
    this.submissionValidationService = submissionValidationService;
    this.eventServiceMetricService = eventServiceMetricService;
    this.objectMapper = objectMapper;
    this.sqsVisibilityExtenderProvider = sqsVisibilityExtenderProvider;
  }

  /**
   * Listens for messages submissions from the Data Claims service, and determines how they should
   * be processed.
   *
   * @param message the message containing the (bulk) submission details.
   */
  @SqsListener("${laa.bulk-claim-queue.name}")
  public void receiveSubmissionEvent(Message message) {

    UUID timerRef = UUID.randomUUID();
    eventServiceMetricService.startFileParsingTimer(timerRef);

    String receiptHandle = message.receiptHandle();

    try (SqsVisibilityExtender extender = sqsVisibilityExtenderProvider.getObject()) {
      extender.start(receiptHandle);
      SubmissionEventType submissionEventType = getSubmissionEventType(message);

      processMessageByType(message, submissionEventType);

    } catch (SubmissionEventProcessingException | IllegalArgumentException ex) {
      log.error("Failed to process submission event. messageId={}", message.messageId(), ex);
      throw ex;

    } finally {
      eventServiceMetricService.stopFileParsingTimer(timerRef);
    }
  }

  private SubmissionEventType getSubmissionEventType(Message message) {
    return Optional.ofNullable(message.messageAttributes().get("SubmissionEventType"))
        .map(MessageAttributeValue::stringValue)
        .map(SubmissionEventType::valueOf)
        .orElseThrow(
            () -> new SubmissionEventProcessingException("Submission event type is missing"));
  }

  private void processMessageByType(Message message, SubmissionEventType submissionEventType) {
    switch (submissionEventType) {
      case PARSE_BULK_SUBMISSION -> handleBulkSubmissionMessage(message);
      case VALIDATE_SUBMISSION -> handleSubmissionValidationMessage(message);
      default ->
          throw new SubmissionEventProcessingException(
              "Unsupported submission event type: " + submissionEventType);
    }
  }

  private void handleSubmissionValidationMessage(Message message) {
    try {
      SubmissionValidationMessage submissionValidationMessage =
          objectMapper.readValue(message.body(), SubmissionValidationMessage.class);

      log.info(
          "Received validation request for submission {}",
          submissionValidationMessage.submissionId());
      submissionValidationService.validateSubmission(submissionValidationMessage.submissionId());
    } catch (JsonProcessingException e) {
      throw new SubmissionEventProcessingException(
          "Unable to read submission validation message", e);
    }
  }

  private void handleBulkSubmissionMessage(Message message) {
    try {
      BulkSubmissionMessage bulkSubmissionMessage =
          objectMapper.readValue(message.body(), BulkSubmissionMessage.class);

      log.info(
          "Received bulk submission {}, with {} submissions",
          bulkSubmissionMessage.bulkSubmissionId(),
          bulkSubmissionMessage.submissionIds().size());

      // Loop through submission IDs and parse data for each one
      for (UUID submissionId : bulkSubmissionMessage.submissionIds()) {
        bulkParsingService.parseData(bulkSubmissionMessage.bulkSubmissionId(), submissionId);
      }
    } catch (JsonProcessingException e) {
      throw new SubmissionEventProcessingException(
          "Unable to read parse bulk submission message", e);
    }
  }
}
