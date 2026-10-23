package uk.gov.justice.laa.dstew.payments.claimsevent.maintenance;

import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Settles the in-flight count tracked by {@link ListenerActivityTracker} once a message's
 * acknowledgement (SQS deletion) actually completes - whether it succeeds or fails.
 *
 * <p>{@code SubmissionListener} increments the in-flight count when it starts processing a message,
 * but deliberately leaves it incremented when processing finishes successfully: under {@code
 * AcknowledgementMode.ON_SUCCESS}, the SQS acknowledgement happens asynchronously, after the
 * listener method has already returned. This callback is the counterpart that performs the actual
 * decrement once that acknowledgement is settled, so a preStop/shutdown hook polling the in-flight
 * count can't observe zero while a just-finished message's acknowledgement is still being flushed.
 *
 * <p>A failed acknowledgement is still counted as settled here: the message was not deleted and
 * will be redelivered once its visibility timeout expires, which is the correct/expected outcome,
 * not a reason to block shutdown indefinitely.
 */
@Slf4j
@Component
public class AcknowledgementTrackingCallback implements AcknowledgementResultCallback<Object> {

  private final ListenerActivityTracker activityTracker;

  public AcknowledgementTrackingCallback(ListenerActivityTracker activityTracker) {
    this.activityTracker = activityTracker;
  }

  @Override
  public void onSuccess(Collection<Message<Object>> messages) {
    activityTracker.decrement(messages.size());
  }

  @Override
  public void onFailure(Collection<Message<Object>> messages, Throwable t) {
    log.error("Acknowledgement failed for {} message(s)", messages.size(), t);
    activityTracker.decrement(messages.size());
  }
}
