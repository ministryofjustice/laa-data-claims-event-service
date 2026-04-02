package uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.ShutdownService;
import uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.model.ShutdownStatus;

/**
 * Controller exposing endpoints to control and query the application's graceful shutdown
 * (drain/maintenance) state. Endpoints allow initiating a drain, cancelling it, and viewing current
 * status and readiness. These endpoints are intended for operational use (preStop hooks, admin
 * tooling) and are not part of the public API surface.
 */
@Slf4j
@RestController
@RequestMapping("/actuator/maintenance")
public class ShutdownController {

  private final ShutdownService shutdownService;
  private final Duration defaultTimeout;

  public ShutdownController(
      ShutdownService shutdownService,
      @Value("${management.shutdown.timeout:PT30S}") Duration defaultTimeout) {
    this.shutdownService = shutdownService;
    this.defaultTimeout = defaultTimeout;
  }

  /**
   * Initiate a graceful drain: stop accepting new work and wait for in-flight work to finish. This
   * method is idempotent and will return the current status if a drain is already in progress.
   *
   * @param request the incoming HTTP request; used to extract an optional X-Triggered-By header for
   *     audit purposes
   * @return the current shutdown status
   */
  @PostMapping("/drain")
  public ResponseEntity<ShutdownStatus> drain(HttpServletRequest request) {
    TriggerInfo info = extractTriggerInfo(request);
    log.info(
        "Received drain request via actuator endpoint. triggeredBy={} pod={} ts={}",
        info.actor(),
        info.podName(),
        info.timestamp());

    // If we are already draining, make the action idempotent by returning current status
    if (shutdownService.shutdownInitiated() || !shutdownService.isAcceptingWork()) {
      log.info("Drain already in progress - idempotent return. triggeredBy={}", info.actor());
      return ResponseEntity.ok(
          ShutdownStatus.from(shutdownService, info.podName(), info.timestamp()));
    }

    // Start shutdown/drain and wait (initiateShutdown is blocking up to defaultTimeout)
    shutdownService.initiateShutdown(defaultTimeout);

    return ResponseEntity.ok(
        ShutdownStatus.from(shutdownService, info.podName(), info.timestamp()));
  }

  /**
   * Cancel an in-progress drain and resume accepting new work. Intended for operational recovery or
   * rollback of a previously-initiated maintenance window.
   *
   * @param request the incoming HTTP request; used to extract an optional X-Triggered-By header for
   *     audit purposes
   * @return the current shutdown status after cancel attempt
   */
  @DeleteMapping
  public ResponseEntity<ShutdownStatus> cancel(HttpServletRequest request) {
    TriggerInfo info = extractTriggerInfo(request);

    log.info(
        "Cancel maintenance request received. triggeredBy={} pod={} ts={}",
        info.actor(),
        info.podName(),
        info.timestamp());

    shutdownService.cancelShutdown();

    return ResponseEntity.ok(
        ShutdownStatus.from(shutdownService, info.podName(), info.timestamp()));
  }

  /**
   * Returns the current shutdown/drain status including whether a drain is in progress, the number
   * of in-flight messages, and whether the application is currently accepting new messages.
   *
   * @param request the incoming HTTP request; used to extract an optional X-Triggered-By header for
   *     audit purposes (not required)
   * @return the current shutdown status
   */
  @GetMapping("/status")
  public ResponseEntity<ShutdownStatus> status(HttpServletRequest request) {
    TriggerInfo info = extractTriggerInfo(request);
    return ResponseEntity.ok(
        ShutdownStatus.from(shutdownService, info.podName(), info.timestamp()));
  }

  // Helper record and method to extract trigger information for audit logging.
  private record TriggerInfo(String actor, String podName, String timestamp) {}

  private TriggerInfo extractTriggerInfo(HttpServletRequest request) {
    String actor = request.getHeader("X-Triggered-By");
    if (actor == null || actor.isBlank()) {
      actor = request.getRemoteAddr();
    }

    String podName = System.getenv().getOrDefault("HOSTNAME", "unknown-pod");
    String timestamp = Instant.now().toString();

    return new TriggerInfo(actor, podName, timestamp);
  }
}
