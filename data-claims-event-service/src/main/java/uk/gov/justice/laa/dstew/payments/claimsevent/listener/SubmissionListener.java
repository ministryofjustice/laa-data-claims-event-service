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
import uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.ShutdownService;
import uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.ShutdownService.ShutdownGuard;
import uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.exception.ShutdownRejectedException;

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
  private final ShutdownService shutdownService;

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
      ObjectProvider<SqsVisibilityExtender> sqsVisibilityExtenderProvider,
      ShutdownService shutdownServiceProvider) {
    this.bulkParsingService = bulkParsingService;
    this.submissionValidationService = submissionValidationService;
    this.eventServiceMetricService = eventServiceMetricService;
    this.objectMapper = objectMapper;
    this.sqsVisibilityExtenderProvider = sqsVisibilityExtenderProvider;
    this.shutdownService = shutdownServiceProvider;
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

    // Acquire a shutdown guard atomically and reject with a exception if the
    // service is not accepting new work. The supplier is invoked only when rejection is
    // required so we avoid constructing exception objects unnecessarily.
    try (@SuppressWarnings("unused")
            ShutdownGuard shutdownGuard = shutdownService.acquireShutdownGuardOrThrow();
        SqsVisibilityExtender extender = sqsVisibilityExtenderProvider.getObject()) {
      extender.start(receiptHandle);
      SubmissionEventType submissionEventType = getSubmissionEventType(message);

      // sleep(30000);

      processMessageByType(message, submissionEventType);
    } catch (SubmissionEventProcessingException | IllegalArgumentException ex) {
      throw ex;
    } catch (ShutdownRejectedException ex) {
      log.info(
          "Shutdown in progress - not accepting new messages. messageId={}", message.messageId());
      throw new SubmissionEventProcessingException(
          "Service is shutting down - not accepting messages");
    } catch (Exception ex) {
      log.error(
          "Failed to process submission event. messageId={}. Exception: {}",
          message.messageId(),
          ex.getMessage());
      throw new SubmissionEventProcessingException(
          "Unhandled exception in submission event processing", ex);
    } finally {
      // inFlightScope.close() is handled by try-with-resources; just stop metrics here.
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

      // Loop through submission IDs and parse data for each one. NOTE: we do not catch individual
      // failures any error is caught when parsing the individual submission and it marks the whole
      // bulk submission as failed, so we want to allow any exception to propagate up and be handled
      // by the global exception handler which will mark the message as failed and stop it being
      // retried.
      for (UUID submissionId : bulkSubmissionMessage.submissionIds()) {
        bulkParsingService.parseData(bulkSubmissionMessage.bulkSubmissionId(), submissionId);
      }
    } catch (JsonProcessingException e) {
      throw new SubmissionEventProcessingException(
          "Unable to read parse bulk submission message", e);
    }
  }
}
