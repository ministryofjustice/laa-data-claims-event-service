package uk.gov.justice.laa.dstew.payments.claimsevent.shutdown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ShutdownServiceConcurrencyTest {

  @Test
  void shutdown_waits_for_existing_work_and_rejects_new_acquires() throws Exception {
    ShutdownService shutdownService = new ShutdownService();

    CountDownLatch t1Started = new CountDownLatch(1);
    CountDownLatch t1Release = new CountDownLatch(1);
    AtomicBoolean initiateResult = new AtomicBoolean(false);

    // T1: holds a guard (simulates in-flight processing)
    Thread t1 =
        new Thread(
            () -> {
              try (ShutdownService.ShutdownGuard guard =
                  shutdownService.acquireShutdownGuardOrThrow()) {
                // signal that T1 has started and holds the guard
                t1Started.countDown();
                // wait until main thread allows release
                t1Release.await();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            },
            "T1-processing");

    t1.start();

    // Wait for T1 to have acquired the guard
    boolean started = t1Started.await(1, TimeUnit.SECONDS);
    if (!started) {
      throw new IllegalStateException("T1 did not start in time");
    }

    // Sanity: there should be 1 in-flight
    assertThat(shutdownService.getInFlightCount()).isEqualTo(1);

    // Start initiator thread that will call initiateShutdown and block until drain
    Thread initiator =
        new Thread(
            () -> {
              boolean drained = shutdownService.initiateShutdown(Duration.ofSeconds(5));
              initiateResult.set(drained);
            },
            "initiator");
    initiator.start();

    // Give the initiator a moment to set accepting=false
    TimeUnit.MILLISECONDS.sleep(100);

    // T2: attempt to acquire during shutdown -> should be rejected with supplied exception
    assertThrows(RuntimeException.class, shutdownService::acquireShutdownGuardOrThrow);

    // Now release T1 so it can finish and allow the initiator to complete
    t1Release.countDown();

    // wait for initiator to finish (should finish because T1 will close guard)
    initiator.join(2000);

    // verify the initiator returned drained=true and no in-flight remain
    assertThat(initiateResult.get()).isTrue();
    assertThat(shutdownService.getInFlightCount()).isZero();
  }
}
