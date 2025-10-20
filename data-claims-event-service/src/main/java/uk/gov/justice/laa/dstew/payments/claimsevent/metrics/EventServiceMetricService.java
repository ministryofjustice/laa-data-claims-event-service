package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Service for publishing metrics to Prometheus.
 *
 * @author Jamie Briggs
 */
@Getter
@Component
public class EventServiceMetricService {

  private final Counter totalSubmissionsCreatedCounter;
  private final Counter totalClaimsCreatedCounter;
  private final Counter totalSubmissionsValidatedWithErrorsCounter;
  private final Counter totalClaimsValidatedAndValidCounter;
  private final Counter totalClaimsValidatedAndWarningsFoundCounter;
  private final Counter totalClaimsValidatedAndErrorsFoundCounter;
  private final Counter totalValidSubmissionsCounter;
  private final Counter totalInvalidSubmissionsCounter;

  /**
   * Constructor.
   *
   * @param meterRegistry the Prometheus registry to publish metrics to.
   */
  public EventServiceMetricService(PrometheusRegistry meterRegistry) {
    this.totalSubmissionsCreatedCounter =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_submissions_added")
            .help("Total number of submissions created from message queue")
            .register(meterRegistry);
    this.totalClaimsCreatedCounter =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_claims_added")
            .help("Total number of claims created from submission read in message queue")
            .register(meterRegistry);
    this.totalSubmissionsValidatedWithErrorsCounter =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_submissions_with_errors")
            .help("Total number submissions with validation errors")
            .register(meterRegistry);
    this.totalClaimsValidatedAndValidCounter =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_claims_validated_and_valid")
            .help("Total number of claims validated and valid")
            .register(meterRegistry);
    this.totalClaimsValidatedAndWarningsFoundCounter =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_claims_validated_and_warnings_found")
            .help("Total number of claims validated and have warnings")
            .register(meterRegistry);
    this.totalClaimsValidatedAndErrorsFoundCounter =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_claims_validated_and_invalid")
            .help("Total number of claims validated and invalid")
            .register(meterRegistry);
    this.totalValidSubmissionsCounter =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_valid_submissions")
            .help("Total number submissions which are valid")
            .register(meterRegistry);
    this.totalInvalidSubmissionsCounter =
        io.prometheus.metrics.core.metrics.Counter.builder()
            .name("claims_event_service_invalid_submissions")
            .help("Total number submissions which are invalid")
            .register(meterRegistry);
  }

  /**
   * Increments the total submissions created counter. Should be called when a new submission is
   * created via the Claims API.
   */
  public void incrementTotalSubmissionsCreated() {
    totalSubmissionsCreatedCounter.inc();
  }

  /**
   * Increments the total claims created counter. Should be called when a new claim is created via
   * the Claims API.
   */
  public void incrementTotalClaimsCreated() {
    totalClaimsCreatedCounter.inc();
  }

  /**
   * Increments the total claims validated and valid counter. Should be called when a claim is
   * validated and no messages are found to indicate an error or warning.
   */
  public void incrementTotalClaimsValidatedAndValid() {
    totalClaimsValidatedAndValidCounter.inc();
  }

  /**
   * Increments the total claims validated and warnings found counter. Should be called when a claim
   * is validated and messages are found to indicate a warning.
   */
  public void incrementTotalClaimsValidatedAndWarningsFound() {
    totalClaimsValidatedAndWarningsFoundCounter.inc();
  }

  /**
   * Increments the total claims validated and errors found counter. Should be called when a claim
   * is validated and messages are found to indicate an error.
   */
  public void incrementTotalClaimsValidatedAndErrorsFound() {
    totalClaimsValidatedAndErrorsFoundCounter.inc();
  }

  /**
   * Increments the total submissions validated with submission errors counter. Should be called
   * when a submission is validated and messages are found to indicate an error.
   */
  public void incrementTotalSubmissionsValidatedWithSubmissionErrors() {
    totalSubmissionsValidatedWithErrorsCounter.inc();
  }

  /**
   * Increments the total valid submissions counter. Should be called when a submission is fully
   * finished being validated and the submission is valid (Could also include warnings)
   */
  public void incrementTotalValidSubmissions() {
    totalValidSubmissionsCounter.inc();
  }

  /**
   * Increments the total invalid submissions counter. Should be called when a submission is fully
   * finished being validated and the submission is invalid or rejected.
   */
  public void incrementTotalInvalidSubmissions() {
    totalInvalidSubmissionsCounter.inc();
  }
}
