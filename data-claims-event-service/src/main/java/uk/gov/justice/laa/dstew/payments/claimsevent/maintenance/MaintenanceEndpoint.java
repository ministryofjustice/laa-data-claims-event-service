package uk.gov.justice.laa.dstew.payments.claimsevent.maintenance;

import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

/**
 * Actuator endpoint exposed at {@code /actuator/maintenance}, used to deliberately stop and resume
 * SQS message processing independently of pod termination.
 *
 * <p>This is the mechanism a preStop hook (or an operator, ahead of a manual deployment) uses to
 * stop the SQS listener(s) from polling for new messages, then poll {@link #status()} until
 * in-flight processing has drained to zero before the pod is actually terminated or a deployment
 * proceeds.
 *
 * <ul>
 *   <li>{@code POST /actuator/maintenance} - pause: stop all listener containers
 *   <li>{@code GET /actuator/maintenance} - status: listener running state and in-flight count
 *   <li>{@code DELETE /actuator/maintenance} - resume: restart all listener containers
 * </ul>
 */
@Slf4j
@Endpoint(id = "maintenance")
public class MaintenanceEndpoint {

  private final MessageListenerContainerRegistry containerRegistry;
  private final ListenerActivityTracker activityTracker;
  private final Executor maintenanceExecutor;

  public MaintenanceEndpoint(
      MessageListenerContainerRegistry containerRegistry,
      ListenerActivityTracker activityTracker,
      Executor maintenanceExecutor) {
    this.containerRegistry = containerRegistry;
    this.activityTracker = activityTracker;
    this.maintenanceExecutor = maintenanceExecutor;
  }

  /**
   * Stops every registered SQS listener container so no new messages are taken off the queue.
   * Returns immediately rather than waiting for in-flight work to drain - callers should poll
   * {@link #status()} to observe when polling has actually stopped and in-flight work has finished.
   *
   * @return the maintenance status at the moment pause was requested
   */
  @WriteOperation
  public MaintenanceStatus pause() {
    log.info("Maintenance pause requested - stopping SQS listener containers");
    maintenanceExecutor.execute(
        () -> containerRegistry.getListenerContainers().forEach(MessageListenerContainer::stop));
    return status();
  }

  /**
   * Restarts every registered SQS listener container so polling for new messages resumes. Used to
   * recover from a pause that is no longer needed, for example when a planned deployment is aborted
   * before the pod is terminated.
   *
   * @return the maintenance status after resume was requested
   */
  @DeleteOperation
  public MaintenanceStatus resume() {
    log.info("Maintenance resume requested - restarting SQS listener containers");
    containerRegistry.getListenerContainers().forEach(MessageListenerContainer::start);
    return status();
  }

  /**
   * Reports whether any listener container is still running (polling), and the current in-flight
   * message count.
   *
   * @return the current maintenance status
   */
  @ReadOperation
  public MaintenanceStatus status() {
    boolean listenersRunning =
        containerRegistry.getListenerContainers().stream()
            .anyMatch(MessageListenerContainer::isRunning);
    return new MaintenanceStatus(listenersRunning, activityTracker.getInFlightCount());
  }
}
