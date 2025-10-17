package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.stereotype.Component;

/**
 * Service for publishing metrics to Prometheus.
 *
 * @author Jamie Briggs
 */
@Component
public class EventServiceMetricService {
  private final Counter submissionsCreated;
  private final Counter claimsCreated;
  private final Counter submissionsValidatedWithSubmissionErrorsCounter;
  private final Counter claimsValidatedAndValid;
  private final Counter claimValidatedAndWarningsFound;
  private final Counter claimValidatedAndErrorsFound;

  /**
   * Constructor.
   *
   * @param meterRegistry the Prometheus registry to publish metrics to.
   */
  public EventServiceMetricService(PrometheusRegistry meterRegistry) {
    this.submissionsCreated =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_submissions_read")
            .help("Total number of submissions created from message queue")
            .register(meterRegistry);
    this.claimsCreated =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_claims_read")
            .help("Total number of claims created from submission read in message queue")
            .register(meterRegistry);
    this.submissionsValidatedWithSubmissionErrorsCounter =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_submission_with_errors")
            .help("Total number of claims created")
            .register(meterRegistry);
    this.claimsValidatedAndValid =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_claims_validated_and_valid")
            .help("Total number of claims validated and valid")
            .register(meterRegistry);
    this.claimValidatedAndWarningsFound =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_claims_validated_and_warnings_found")
            .help("Total number of claims validated and invalid")
            .register(meterRegistry);
    this.claimValidatedAndErrorsFound =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_claims_validated_and_invalid")
            .help("Total number of claims validated and invalid")
            .register(meterRegistry);
  }

  public void incrementTotalSubmissionsCreated() {
    submissionsCreated.inc();
  }

  public void incrementClaimCreated() {
    claimsCreated.inc();
  }

  public void incrementClaimValidatedAndValid() {
    claimsValidatedAndValid.inc();
  }

  public void incrementClaimValidatedAndWarningsFound() {
    claimValidatedAndWarningsFound.inc();
  }

  public void incrementClaimValidatedAndErrorsFound() {
    claimValidatedAndErrorsFound.inc();
  }

  public void incrementSubmissionValidatedWithSubmissionErrors() {
    submissionsValidatedWithSubmissionErrorsCounter.inc();
  }
}
