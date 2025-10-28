package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;

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

  private final Counter warningTypeCounter;
  private final Counter errorTypeCounter;

  private final Histogram submissionValidationTimeHistogram;
  private final Histogram claimValidationTimeHistogram;
  private final Histogram fspValidationTimeHistogram;

  private final String METRIC_NAMESPACE = "claims_event_service_";

  /**
   * Constructor.
   *
   * @param meterRegistry the Prometheus registry to publish metrics to.
   */
  public EventServiceMetricService(PrometheusRegistry meterRegistry) {
    this.totalSubmissionsCreatedCounter =
        Counter.builder()
            .name(METRIC_NAMESPACE + "submissions_added")
            .help("Total number of submissions created from message queue")
            .register(meterRegistry);
    this.totalClaimsCreatedCounter =
        Counter.builder()
            .name(METRIC_NAMESPACE + "claims_added")
            .help("Total number of claims created from submission read in message queue")
            .register(meterRegistry);
    this.totalSubmissionsValidatedWithErrorsCounter =
        Counter.builder()
            .name(METRIC_NAMESPACE + "submissions_with_errors")
            .help("Total number submissions with validation errors")
            .register(meterRegistry);
    this.totalClaimsValidatedAndValidCounter =
        Counter.builder()
            .name(METRIC_NAMESPACE + "claims_validated_and_valid")
            .help("Total number of claims validated and valid")
            .register(meterRegistry);
    this.totalClaimsValidatedAndWarningsFoundCounter =
        Counter.builder()
            .name(METRIC_NAMESPACE + "claims_validated_and_warnings_found")
            .help("Total number of claims validated and have warnings")
            .register(meterRegistry);
    this.totalClaimsValidatedAndErrorsFoundCounter =
        Counter.builder()
            .name(METRIC_NAMESPACE + "claims_validated_and_invalid")
            .help("Total number of claims validated and invalid")
            .register(meterRegistry);
    this.totalValidSubmissionsCounter =
        Counter.builder()
            .name(METRIC_NAMESPACE + "valid_submissions")
            .help("Total number submissions which are valid")
            .register(meterRegistry);
    this.totalInvalidSubmissionsCounter =
        Counter.builder()
            .name(METRIC_NAMESPACE + "invalid_submissions")
            .help("Total number submissions which are invalid")
            .register(meterRegistry);

    this.errorTypeCounter =
        Counter.builder()
            .name(METRIC_NAMESPACE + "messages_errors")
            .help("Different types of messages found by the event service")
            .labelNames("error_source", "type", "message")
            .register(meterRegistry);
    this.warningTypeCounter =
        Counter.builder()
            .name(METRIC_NAMESPACE + "messages_warnings")
            .help("Different types of warning messages found by the event service")
            .labelNames("error_source", "type", "message")
            .register(meterRegistry);

    this.submissionValidationTimeHistogram = Histogram.builder()
        .name(METRIC_NAMESPACE + "submission_validation_time")
        .help("Total time taken to validate claim (Include FSP validation time)")
        .register(meterRegistry);
    this.claimValidationTimeHistogram = Histogram.builder()
        .name(METRIC_NAMESPACE + "claim_validation_time")
        .help("Total time taken to validate claim (Including FSP validation time)")
        .register(meterRegistry);
    this.fspValidationTimeHistogram = Histogram.builder()
        .name(METRIC_NAMESPACE + "fsp_validation_time")
        .help("Total time taken to perform fee scheme platform calculation")
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

  public void recordValidationMessage(ValidationMessagePatch validationMessagePatch, boolean isClaim) {
    String type = Boolean.TRUE.equals(isClaim) ? "Claim" : "Submission";
    if (validationMessagePatch.getType() == ValidationMessageType.ERROR) {
      incrementErrorType(
          validationMessagePatch.getSource(), type, validationMessagePatch.getTechnicalMessage());
    } else {
      incrementWarningType(
          validationMessagePatch.getSource(), type, validationMessagePatch.getTechnicalMessage());
    }
  }

  private void incrementWarningType(String source, String type, String warningType) {
    warningTypeCounter.labelValues(source, type, warningType).inc();
  }

  private void incrementErrorType(String source,  String type, String errorType) {
    errorTypeCounter.labelValues(source, type, errorType).inc();
  }
}
