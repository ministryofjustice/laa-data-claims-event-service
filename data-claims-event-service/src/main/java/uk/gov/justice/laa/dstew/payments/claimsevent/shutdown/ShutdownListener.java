package uk.gov.justice.laa.dstew.payments.claimsevent.shutdown;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * Listens for Spring context close events (which happen on SIGTERM) and triggers a graceful
 * shutdown drain so the application stops accepting new messages and waits for in-flight work.
 */
@Slf4j
@Component
public class ShutdownListener implements ApplicationListener<ContextClosedEvent> {

  private final ShutdownService shutdownService;
  private final Duration shutdownTimeout;
  private final AtomicBoolean drainTriggered = new AtomicBoolean(false);

  public ShutdownListener(
      ShutdownService shutdownService,
      @Value("${management.shutdown.timeout:PT30S}") Duration shutdownTimeout) {
    this.shutdownService = shutdownService;
    this.shutdownTimeout = shutdownTimeout;
  }

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {

    // Ignore child contexts (management or servlet child contexts) — we only want to act once for
    // the root application context.
    if (event.getApplicationContext().getParent() != null) {
      return;
    }

    // Ensure the drain is only triggered once even if multiple close events reach the listener.
    if (!drainTriggered.compareAndSet(false, true)) {
      log.info("Shutdown drain already triggered; ignoring duplicate ContextClosedEvent");
      return;
    }

    log.info(
        "ContextClosedEvent received - initiating graceful drain with timeout={}", shutdownTimeout);

    boolean drained = shutdownService.initiateShutdown(shutdownTimeout);
    if (!drained) {
      log.warn(
          "Context shutdown: drain timed out, continuing shutdown with inFlightCount={}",
          shutdownService.getInFlightCount());
    } else {
      log.info("Context shutdown: drained successfully - continuing shutdown");
    }
  }
}
