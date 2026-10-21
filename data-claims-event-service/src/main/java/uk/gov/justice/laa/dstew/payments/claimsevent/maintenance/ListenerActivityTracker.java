package uk.gov.justice.laa.dstew.payments.claimsevent.maintenance;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Tracks the number of SQS listener messages currently being processed (in-flight).
 *
 * <p>This is a plain, thread-safe counter - not a gate or guard. Admission control (stopping new
 * messages being taken off the queue) is handled separately by stopping the listener containers
 * themselves; this tracker only exists to make in-flight activity observable, for example so a
 * preStop hook can poll {@link #getInFlightCount()} until it reaches zero after polling has been
 * stopped.
 */
@Component
public class ListenerActivityTracker {

  private final AtomicInteger inFlightCount = new AtomicInteger(0);

  /** Marks the start of processing a message, incrementing the in-flight count. */
  public void increment() {
    inFlightCount.incrementAndGet();
  }

  /** Marks the end of processing a message, decrementing the in-flight count. */
  public void decrement() {
    inFlightCount.updateAndGet(current -> Math.max(0, current - 1));
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
