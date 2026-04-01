package uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.model;

import uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.ShutdownService;

/** DTO representing shutdown/drain status returned by the actuator endpoints. */
public record ShutdownStatus(
    boolean draining, int inFlightCount, boolean acceptingMessages, String pod, String timestamp) {

  /**
   * Create a ShutdownStatus DTO from the provided service state.
   *
   * @param service the shutdown service providing current state
   * @param pod the pod identifier (for audit)
   * @param timestamp an ISO-8601 timestamp for the status
   * @return a populated ShutdownStatus instance
   */
  public static ShutdownStatus from(ShutdownService service, String pod, String timestamp) {
    return new ShutdownStatus(
        service.shutdownInitiated(),
        service.getInFlightCount(),
        service.isAcceptingWork(),
        pod,
        timestamp);
  }
}
