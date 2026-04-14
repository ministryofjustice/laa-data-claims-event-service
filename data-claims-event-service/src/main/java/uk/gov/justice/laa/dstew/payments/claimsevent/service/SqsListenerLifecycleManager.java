package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.ShutdownHookRegistry;

/**
 * Minimal lifecycle manager that stops awspring SQS listener containers/pollers on this JVM.
 *
 * <p>This class is intentionally explicit and conservative: it only operates on beans of type
 * {@link io.awspring.cloud.sqs.listener.SqsMessageListenerContainer} and does not attempt broad or
 * reflective fallbacks to avoid stopping unrelated components.
 */
@Slf4j
@Component
public class SqsListenerLifecycleManager {

  private final ApplicationContext ctx;

  /**
   * Construct a new lifecycle manager and register its start/stop actions with the provided {@link
   * uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.ShutdownHookRegistry}.
   *
   * @param ctx application context used to locate listener container beans
   * @param hooks registry where lifecycle hooks will be registered
   */
  public SqsListenerLifecycleManager(ApplicationContext ctx, ShutdownHookRegistry hooks) {
    this.ctx = ctx;

    // Register hooks so this manager is invoked during shutdown/cancel phases
    hooks.registerOnShutdown(this::stopAllListeners);
    hooks.registerOnCancel(this::startAllListeners);
  }

  /** Stop any awspring SQS listener container beans found in the application context. */
  public void stopAllListeners() {
    log.info("Stopping awspring SQS listener containers on this JVM...");

    Map<String, ?> containers = ctx.getBeansOfType(SqsMessageListenerContainer.class);
    if (!containers.isEmpty()) {
      containers
          .values()
          .forEach(
              o -> {
                SqsMessageListenerContainer<?> c = (SqsMessageListenerContainer<?>) o;
                try {
                  log.info(
                      "Stopping awspring SqsMessageListenerContainer: {}", c.getClass().getName());
                  c.stop();
                } catch (Exception e) {
                  log.warn(
                      "Failed to stop awspring container {}: {}",
                      c.getClass().getName(),
                      e.getMessage(),
                      e);
                }
              });
    } else {
      log.debug("No awspring SqsMessageListenerContainer beans found");
    }
  }

  /** Best-effort start of previously stopped listener containers. */
  public void startAllListeners() {
    log.info("Starting SQS listener containers/pollers on this JVM...");
    Map<String, ?> containers = ctx.getBeansOfType(SqsMessageListenerContainer.class);
    if (!containers.isEmpty()) {
      containers
          .values()
          .forEach(
              o -> {
                SqsMessageListenerContainer<?> c = (SqsMessageListenerContainer<?>) o;
                try {
                  log.info(
                      "Starting awspring SqsMessageListenerContainer: {}", c.getClass().getName());
                  c.start();
                } catch (Exception e) {
                  log.warn(
                      "Failed to start awspring container {}: {}",
                      c.getClass().getName(),
                      e.getMessage(),
                      e);
                }
              });
    } else {
      log.debug("No awspring SqsMessageListenerContainer beans found");
    }
    log.info("SQS listener start attempt complete.");
  }
}
