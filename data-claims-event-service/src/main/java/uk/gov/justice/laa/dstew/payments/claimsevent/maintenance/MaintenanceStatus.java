package uk.gov.justice.laa.dstew.payments.claimsevent.maintenance;

/**
 * Snapshot of the SQS listener's maintenance/drain state, returned by the {@code maintenance}
 * actuator endpoint.
 *
 * @param listenersRunning whether at least one SQS listener container is currently running (polling
 *     for new messages)
 * @param inFlightCount the number of messages currently being processed
 */
public record MaintenanceStatus(boolean listenersRunning, int inFlightCount) {}
