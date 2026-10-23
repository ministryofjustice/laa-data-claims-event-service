package uk.gov.justice.laa.dstew.payments.claimsevent.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MaintenanceEndpoint")
class MaintenanceEndpointTest {

  private MessageListenerContainer<?> container;
  private ListenerActivityTracker activityTracker;
  private MaintenanceEndpoint maintenanceEndpoint;

  @BeforeEach
  void setUp() {
    MessageListenerContainerRegistry containerRegistry =
        mock(MessageListenerContainerRegistry.class);
    container = mock(MessageListenerContainer.class);
    when(containerRegistry.getListenerContainers()).thenReturn(List.of(container));
    activityTracker = new ListenerActivityTracker();

    // Run the "pause" executor synchronously so assertions can observe its effect immediately.
    Executor synchronousExecutor = Runnable::run;
    maintenanceEndpoint =
        new MaintenanceEndpoint(containerRegistry, activityTracker, synchronousExecutor);
  }

  @Test
  @DisplayName("status reports listenersRunning=true and the current in-flight count")
  void statusReportsRunningAndInFlightCount() {
    when(container.isRunning()).thenReturn(true);
    activityTracker.increment();
    activityTracker.increment();

    MaintenanceStatus status = maintenanceEndpoint.status();

    assertThat(status.listenersRunning()).isTrue();
    assertThat(status.inFlightCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("status reports listenersRunning=false once all containers have stopped")
  void statusReportsNotRunningOnceStopped() {
    when(container.isRunning()).thenReturn(false);

    MaintenanceStatus status = maintenanceEndpoint.status();

    assertThat(status.listenersRunning()).isFalse();
  }

  @Test
  @DisplayName("pause stops every listener container and returns the resulting status")
  void pauseStopsEveryContainer() {
    doAnswer(_ -> null).when(container).stop();
    when(container.isRunning()).thenReturn(false);

    MaintenanceStatus status = maintenanceEndpoint.pause();

    verify(container).stop();
    assertThat(status.listenersRunning()).isFalse();
  }

  @Test
  @DisplayName("resume starts every listener container and returns the resulting status")
  void resumeStartsEveryContainer() {
    doAnswer(_ -> null).when(container).start();
    when(container.isRunning()).thenReturn(true);

    MaintenanceStatus status = maintenanceEndpoint.resume();

    verify(container).start();
    assertThat(status.listenersRunning()).isTrue();
  }
}
