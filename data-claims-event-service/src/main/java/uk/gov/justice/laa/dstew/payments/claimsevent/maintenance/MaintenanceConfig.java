package uk.gov.justice.laa.dstew.payments.claimsevent.maintenance;

import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for the maintenance/drain actuator endpoint and its supporting beans. */
@Configuration
public class MaintenanceConfig {

  /**
   * A small, dedicated single-thread executor used to run the (bounded, but potentially
   * multi-second) listener container stop operation off the HTTP request thread handling the
   * {@code pause} actuator call, so that call returns immediately.
   *
   * @return the executor
   */
  @Bean
  public Executor maintenanceExecutor() {
    return Executors.newSingleThreadExecutor(
        runnable -> {
          Thread thread = new Thread(runnable, "maintenance-pause");
          thread.setDaemon(true);
          return thread;
        });
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "spring.cloud.aws.sqs",
      name = "enabled",
      matchIfMissing = true)
  public MaintenanceEndpoint maintenanceEndpoint(
      MessageListenerContainerRegistry containerRegistry,
      ListenerActivityTracker activityTracker,
      Executor maintenanceExecutor) {
    return new MaintenanceEndpoint(containerRegistry, activityTracker, maintenanceExecutor);
  }
}
