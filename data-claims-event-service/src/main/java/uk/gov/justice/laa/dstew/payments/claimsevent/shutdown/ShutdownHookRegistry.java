package uk.gov.justice.laa.dstew.payments.claimsevent.shutdown;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Registry for shutdown/cancel hooks. Components can register actions to be executed when a
 * graceful shutdown is initiated or cancelled.
 */
@Slf4j
@Component
public class ShutdownHookRegistry {

  private final List<Runnable> onShutdownActions = new CopyOnWriteArrayList<>();
  private final List<Runnable> onCancelActions = new CopyOnWriteArrayList<>();

  // TODO (improvements):
  // - Add hook priorities: allow registration with an integer priority so critical hooks run first.
  // - Per-hook timeout and optional async execution: run hooks with an executor and give each a
  //   configurable timeout so slow/blocked hooks cannot stall shutdown; report failures.
  // - Deregistration handle: return an AutoCloseable or token when registering so callers can
  //   unregister hooks if needed.
  // - Idempotency guidance: document/ensure hooks are idempotent; consider protecting against
  //   multiple invocations.
  // - Structured results: allow hooks to return success/failure or diagnostic info instead of
  //   just logging exceptions, so orchestrators can make informed decisions.

  /** Register an action to run when shutdown is initiated. */
  public void registerOnShutdown(Runnable action) {
    onShutdownActions.add(action);
  }

  /** Register an action to run when shutdown is cancelled. */
  public void registerOnCancel(Runnable action) {
    onCancelActions.add(action);
  }

  /**
   * Execute registered on-shutdown actions in registration order. Best-effort — exceptions are
   * logged.
   */
  public void runOnShutdown() {
    log.info("Running {} on-shutdown hooks", onShutdownActions.size());
    for (Runnable r : onShutdownActions) {
      try {
        r.run();
      } catch (Exception e) {
        log.warn("Exception running on-shutdown hook: {}", e.getMessage(), e);
      }
    }
  }

  /** Execute registered on-cancel actions in registration order. */
  public void runOnCancel() {
    log.info("Running {} on-cancel hooks", onCancelActions.size());
    for (Runnable r : onCancelActions) {
      try {
        r.run();
      } catch (Exception e) {
        log.warn("Exception running on-cancel hook: {}", e.getMessage(), e);
      }
    }
  }
}
