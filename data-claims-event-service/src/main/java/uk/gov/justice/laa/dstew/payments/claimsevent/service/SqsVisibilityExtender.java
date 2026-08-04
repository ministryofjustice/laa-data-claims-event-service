package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

/** Extends the visibility timeout of an SQS message. */
@Slf4j
@Service
@Scope(SCOPE_PROTOTYPE)
public class SqsVisibilityExtender implements AutoCloseable {

  private final SqsClient sqsClient;
  private final int visibilityTimeoutSeconds;
  private final int visibilityExtensionIntervalSeconds;
  private final ScheduledExecutorService scheduler;
  private final String queueName;
  private ScheduledFuture<?> scheduledTask;
  private String receiptHandle;
  private String queueUrl;
  private boolean completed;

  /**
   * Constructs an instance of SqsVisibilityExtender.
   *
   * @param sqsClient the SqsClient to interact with Amazon SQS
   * @param queueName the name of the SQS queue
   * @param visibilityTimeoutSeconds the visibility timeout in seconds for the SQS message
   * @param visibilityExtensionIntervalSeconds the interval in seconds at which the visibility
   *     timeout should be extended
   */
  public SqsVisibilityExtender(
      final SqsClient sqsClient,
      final @Value("${laa.bulk-claim-queue.name}") String queueName,
      final @Value("${laa.bulk-claim-queue.visibility-timeout-seconds:600}") int
              visibilityTimeoutSeconds,
      final @Value("${laa.bulk-claim-queue.visibility-extension-interval-seconds:10}") int
              visibilityExtensionIntervalSeconds) {
    this.sqsClient = sqsClient;
    this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
    this.visibilityExtensionIntervalSeconds = visibilityExtensionIntervalSeconds;
    this.scheduler = Executors.newScheduledThreadPool(1);
    this.queueName = queueName;
  }

  /**
   * Marks processing as successfully completed so that {@link #close()} does not reset the message
   * visibility. Call this immediately before the try-with-resources block exits normally.
   */
  public void markCompleted() {
    this.completed = true;
  }

  /**
   * Start the visibility extender for a specific message.
   *
   * @param receiptHandle the receipt handle of the message to extend visibility for
   */
  public void start(final String receiptHandle) {
    this.receiptHandle = receiptHandle;
    this.queueUrl =
        sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();

    log.debug("Starting visibility extender for receiptHandle={}", receiptHandle);

    scheduledTask =
        scheduler.scheduleAtFixedRate(
            this::extendVisibility,
            0,
            Duration.ofSeconds(visibilityExtensionIntervalSeconds).toSeconds(),
            TimeUnit.SECONDS);
  }

  private void extendVisibility() {
    try {
      log.debug(
          "Extending SQS visibility for receiptHandle={} by {} seconds",
          receiptHandle,
          visibilityTimeoutSeconds);

      ChangeMessageVisibilityRequest request =
          ChangeMessageVisibilityRequest.builder()
              .queueUrl(queueUrl)
              .receiptHandle(receiptHandle)
              .visibilityTimeout(visibilityTimeoutSeconds)
              .build();
      sqsClient.changeMessageVisibility(request);
    } catch (Exception ex) {
      log.error("Failed to extend SQS visibility for receiptHandle={}", receiptHandle, ex);
    }
  }

  @Override
  public void close() {
    if (scheduledTask != null) {
      scheduledTask.cancel(true);
    }
    scheduler.shutdownNow();
    try {
      scheduler.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    if (!completed && receiptHandle != null && queueUrl != null) {
      /*
       * resets the SQS message visibility to 0 if processing did not complete normally
       */
      try {
        log.debug(
            "Processing did not complete normally; resetting SQS visibility to 0 for"
                + " receiptHandle={}",
            receiptHandle);
        sqsClient.changeMessageVisibility(
            ChangeMessageVisibilityRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .visibilityTimeout(0)
                .build());
      } catch (Exception ex) {
        log.warn(
            "Failed to reset SQS visibility for receiptHandle={}: {}",
            receiptHandle,
            ex.getMessage());
      }
    }
  }
}
