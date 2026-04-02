package uk.gov.justice.laa.dstew.payments.claimsevent.shutdown;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.exception.ShutdownRejectedException;

/**
 * Simple service to coordinate graceful shutdown/draining of processing work.
 *
 * <p>Usage: - Call {@link #isAcceptingWork()} before starting new work. When shutdown is initiated
 * the service will stop accepting new work. - Use {@link #acquireShutdownGuardOrThrow()} to
 * atomically reserve and track a unit of work (returns a guard that will decrement the counter on
 * close). - Call {@link #initiateShutdown(Duration)} to stop accepting new work and wait until
 * in-flight work completes (or the timeout elapses).
 */
@Slf4j
@Service
public class ShutdownService {

  private final AtomicBoolean acceptingWork = new AtomicBoolean(true);
  private final AtomicBoolean draining = new AtomicBoolean(false);
  private final AtomicInteger inFlightCount = new AtomicInteger(0);
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition drainedCondition = lock.newCondition();

  /**
   * A small AutoCloseable guard returned to callers to mark a unit of processing work. When the
   * returned guard is closed it will decrement the in-flight counter.
   */
  public interface ShutdownGuard extends AutoCloseable {
    /**
     * Close the guard. Implementations should be idempotent and must not throw checked exceptions
     * so the guard can be used in a try-with-resources block without extra handling.
     */
    @Override
    void close();
  }

  /** Returns true when the application is still accepting new messages. */
  public boolean isAcceptingWork() {
    return acceptingWork.get();
  }

  /** Returns true when a drain/shutdown has been initiated. */
  public boolean shutdownInitiated() {
    return draining.get();
  }

  /** Returns true when there are no in-flight messages. */
  public boolean isDrained() {
    return getInFlightCount() == 0;
  }

  /** Number of in-flight messages currently being processed. */
  public int getInFlightCount() {
    return inFlightCount.get();
  }

  /**
   * Increment the in-flight counter. Internal only — callers should use {@link
   * #acquireShutdownGuardOrThrow()} to obtain a guard which increments/decrements automatically.
   */
  private void incrementInFlightCount() {
    int current = inFlightCount.incrementAndGet();
    log.info("In-flight count incremented to {}", current);
  }

  /** Decrement the in-flight counter. Call when finished processing a message. */
  private void decrementInFlightCount() {
    int current = inFlightCount.decrementAndGet();
    if (current < 0) {
      inFlightCount.set(0);
      log.warn("In-flight count went negative; reset to 0");
    } else {
      log.info("In-flight count decremented to {}", current);
    }

    // If count reaches zero, signal any thread waiting for drains.
    if (current == 0) {
      lock.lock();
      try {
        drainedCondition.signalAll();
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * Attempt to acquire a shutdown guard, or throw a {@link ShutdownRejectedException} if shutdown
   * is active.
   *
   * <p>This convenience method throws a standard unchecked {@link ShutdownRejectedException}
   * containing a generic message. Callers may catch {@code ShutdownRejectedException} and translate
   * it into their own application-level exceptions if desired.
   *
   * @return a {@link ShutdownGuard} that will decrement the in-flight counter on close
   * @throws ShutdownRejectedException when shutdown has been initiated and new work is not accepted
   */
  public ShutdownGuard acquireShutdownGuardOrThrow() {
    lock.lock();
    try {
      if (!acceptingWork.get()) {
        throw new ShutdownRejectedException("Service is shutting down - not accepting work");
      }

      incrementInFlightCount();

      return new ShutdownGuard() {
        private final AtomicBoolean closed = new AtomicBoolean(false);

        @Override
        public void close() {
          if (closed.compareAndSet(false, true)) {
            decrementInFlightCount();
          }
        }
      };
    } finally {
      lock.unlock();
    }
  }

  /**
   * Initiate shutdown: stop accepting new work and wait for in-flight work to complete up to the
   * given timeout. Returns true if drained before timeout.
   */
  public boolean initiateShutdown(Duration timeout) {
    log.info("Initiating graceful shutdown. timeout={}", timeout);
    // Set flags under lock to prevent races with acquireShutdownGuard()
    lock.lock();
    try {
      acceptingWork.set(false);
      draining.set(true);
    } finally {
      lock.unlock();
    }

    long nanos = timeout.toNanos();
    lock.lock();
    try {
      while (inFlightCount.get() > 0 && nanos > 0L) {
        try {
          nanos = drainedCondition.awaitNanos(nanos);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("Drain interrupted");
          break;
        }
      }

      boolean drained = isDrained();
      if (drained) {
        log.info("Drained: no in-flight work remains");
      } else {
        log.warn(
            "Shutdown timed out with {} in-flight work items still running", inFlightCount.get());
      }

      draining.set(!drained);
      return drained;
    } finally {
      lock.unlock();
    }
  }

  /** Cancel an in-progress shutdown/drain and resume accepting new work. */
  public void cancelShutdown() {
    log.info("Cancelling graceful shutdown/drain and resuming accepting work");
    lock.lock();
    try {
      draining.set(false);
      acceptingWork.set(true);
      // signal in case initiateShutdown is waiting
      drainedCondition.signalAll();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Log a message when the application is ready so operators know the monitoring/drain service is
   * active. This uses ApplicationReadyEvent to ensure the application context is fully initialised.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    log.info(
        "Application ready - graceful shutdown monitoring active. acceptingWork={} inFlightCount={}",
        acceptingWork.get(),
        inFlightCount.get());
  }
}
