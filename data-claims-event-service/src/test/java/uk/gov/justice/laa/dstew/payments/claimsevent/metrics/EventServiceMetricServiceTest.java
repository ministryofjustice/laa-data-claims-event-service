package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Event service metric service test")
class EventServiceMetricServiceTest {

  @Mock PrometheusRegistry prometheusRegistry;

  EventServiceMetricService eventServiceMetricService;

  @BeforeEach
  void beforeEach() {
    eventServiceMetricService = new EventServiceMetricService(prometheusRegistry);
  }

  @Test
  @DisplayName("Verify counters initialized")
  void verifyCounterInitialized() {
    // Then
    verify(prometheusRegistry, times(1))
        .register(eventServiceMetricService.getTotalSubmissionsCreatedCounter());
    verify(prometheusRegistry, times(1))
        .register(eventServiceMetricService.getTotalClaimsCreatedCounter());
    verify(prometheusRegistry, times(1))
        .register(eventServiceMetricService.getTotalSubmissionsValidatedWithErrorsCounter());
    verify(prometheusRegistry, times(1))
        .register(eventServiceMetricService.getTotalClaimsValidatedAndValidCounter());
    verify(prometheusRegistry, times(1))
        .register(eventServiceMetricService.getTotalClaimsValidatedAndWarningsFoundCounter());
    verify(prometheusRegistry, times(1))
        .register(eventServiceMetricService.getTotalClaimsValidatedAndErrorsFoundCounter());
    verify(prometheusRegistry, times(1))
        .register(eventServiceMetricService.getTotalValidSubmissionsCounter());
    verify(prometheusRegistry, times(1))
        .register(eventServiceMetricService.getTotalInvalidSubmissionsCounter());
  }

  @Test
  @DisplayName("Should increment total submissions created counter")
  void shouldIncrementTotalSubmissionsCreatedCounter() {
    // Given / When
    eventServiceMetricService.incrementTotalSubmissionsCreated();
    // Then
    assertThat(eventServiceMetricService.getTotalSubmissionsCreatedCounter().get()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment total claims created counter")
  void shouldIncremmentTotalClaimsCreatedCounter() {
    // Given / When
    eventServiceMetricService.incrementTotalClaimsCreated();
    // Then
    assertThat(eventServiceMetricService.getTotalClaimsCreatedCounter().get()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment total claims validated and valid counter")
  void shouldIncrementTotalClaimsValidatedAndValidCounter() {
    // Given / When
    eventServiceMetricService.incrementTotalClaimsValidatedAndValid();
    // Then
    assertThat(eventServiceMetricService.getTotalClaimsValidatedAndValidCounter().get())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment total claims validated and warnings found counter")
  void shouldIncrementTotalClaimsValidatedAndWarningsFoundCounter() {
    // Given / When
    eventServiceMetricService.incrementTotalClaimsValidatedAndWarningsFound();
    // Then
    assertThat(eventServiceMetricService.getTotalClaimsValidatedAndWarningsFoundCounter().get())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment total claims validated and errors found counter")
  void shouldIncrementTotalClaimsValidatedAndErrorsFoundCounter() {
    // Given / When
    eventServiceMetricService.incrementTotalClaimsValidatedAndErrorsFound();
    // Then
    assertThat(eventServiceMetricService.getTotalClaimsValidatedAndErrorsFoundCounter().get())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment total claims validated with submission errors")
  void shouldIncrementTotalSubmissionsValidatedWithSubmissionErrors() {
    // Given / When
    eventServiceMetricService.incrementTotalSubmissionsValidatedWithSubmissionErrors();
    // Then
    assertThat(eventServiceMetricService.getTotalSubmissionsValidatedWithErrorsCounter().get())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment total valid submissions counter")
  void shouldIncrementTotalValidSubmissionsCounter() {
    // Given / When
    eventServiceMetricService.incrementTotalValidSubmissions();
    // Then
    assertThat(eventServiceMetricService.getTotalValidSubmissionsCounter().get()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment total invalid submissions counter")
  void shouldIncrementTotalInvalidSubmissionsCounter() {
    // Given / When
    eventServiceMetricService.incrementTotalInvalidSubmissions();
    // Then
    assertThat(eventServiceMetricService.getTotalInvalidSubmissionsCounter().get()).isEqualTo(1);
  }
}
