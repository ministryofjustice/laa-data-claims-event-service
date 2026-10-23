package uk.gov.justice.laa.dstew.payments.claimsevent.maintenance;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Tracks the number of SQS messages currently "unsettled" - either still being processed, or
 * finished processing but not yet acknowledged (in-flight).
 *
 * <p>This is a plain, thread-safe counter - not a gate or guard. Admission control (stopping new
 * messages being taken off the queue) is handled separately by stopping the listener containers
 * themselves; this tracker only exists to make in-flight activity observable, for example so a
 * preStop hook can poll {@link #getInFlightCount()} until it reaches zero after polling has been
 * stopped.
 *
 * <p>A message is only settled - and the count decremented - once its SQS acknowledgement
 * (deletion) actually completes, not merely once the listener method returns. Under {@code
 * AcknowledgementMode.ON_SUCCESS}, acknowledgement happens asynchronously after the listener method
 * returns, so treating "processing finished" as "safe to terminate" could otherwise race the
 * in-flight message's own acknowledgement and risk redelivering a message that was in fact already
 * fully processed. See {@code AcknowledgementTrackingCallback}, which performs this decrement once
 * acknowledgement is actually settled (whether it succeeds or fails).
 */
@Component
public class ListenerActivityTracker {

  private final AtomicInteger inFlightCount = new AtomicInteger(0);

  /** Marks the start of processing a message, incrementing the in-flight count. */
  public void increment() {
    inFlightCount.incrementAndGet();
  }

  /**
   * Marks a message as settled, decrementing the in-flight count by one. Used when a message's
   * processing fails outright, in which case {@code AcknowledgementMode.ON_SUCCESS} means no
   * acknowledgement will ever be attempted for it, so there is no later callback to settle it.
   */
  public void decrement() {
    decrement(1);
  }

  /**
   * Marks {@code count} messages as settled, decrementing the in-flight count accordingly. Used by
   * {@code AcknowledgementTrackingCallback} once a batch of acknowledgements actually completes
   * (successfully or not).
   *
   * @param count the number of messages that have just settled
   */
  public void decrement(int count) {
    inFlightCount.updateAndGet(current -> Math.max(0, current - count));
  }

  /**
   * Returns the current number of messages being processed.
   *
   * @return the in-flight message count
   */
  public int getInFlightCount() {
    return inFlightCount.get();
  }
}
