package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;

/**
 * Service for publishing metrics to Prometheus.
 *
 * @author Jamie Briggs
 */
@Slf4j
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

  private final Summary submissionValidationTimeSummary;
  private final HashMap<UUID, TimerLifecycle> submissionValidationTimers;
  private final Summary claimValidationTimeSummary;
  private final HashMap<UUID, TimerLifecycle> claimValidationTimers;
  private final Summary fspValidationTimeSummary;
  private final HashMap<UUID, TimerLifecycle> fspValidationTimers;

  private static final String METRIC_NAMESPACE = "claims_event_service_";

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

    this.submissionValidationTimeSummary =
        Summary.builder()
            .name(METRIC_NAMESPACE + "submission_validation_time")
            .help("Total time taken to validate claim (Include FSP validation time)")
            .quantile(0.5, 0.05)   // P50 with 5% error tolerance
            .quantile(0.9, 0.02)   // P90 with 2% error tolerance
            .quantile(0.95, 0.01)  // P95 with 1% error tolerance
            .quantile(0.99, 0.001) // P99 with 0.1% error tolerance
            .register(meterRegistry);
    this.submissionValidationTimers = new HashMap<>();
    this.claimValidationTimeSummary =
        Summary.builder()
            .name(METRIC_NAMESPACE + "claim_validation_time")
            .help("Total time taken to validate claim (Including FSP validation time)")
            .quantile(0.5, 0.05)   // P50 with 5% error tolerance
            .quantile(0.9, 0.02)   // P90 with 2% error tolerance
            .quantile(0.95, 0.01)  // P95 with 1% error tolerance
            .quantile(0.99, 0.001) // P99 with 0.1% error tolerance
            .register(meterRegistry);
    this.claimValidationTimers = new HashMap<>();
    this.fspValidationTimeSummary =
        Summary.builder()
            .name(METRIC_NAMESPACE + "fsp_validation_time")
            .help("Total time taken to perform fee scheme platform calculation")
            .quantile(0.5, 0.05)   // P50 with 5% error tolerance
            .quantile(0.9, 0.02)   // P90 with 2% error tolerance
            .quantile(0.95, 0.01)  // P95 with 1% error tolerance
            .quantile(0.99, 0.001) // P99 with 0.1% error tolerance
            .register(meterRegistry);
    this.fspValidationTimers = new HashMap<>();
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

  /**
   * Records a validation message which has been found with a claim or submission.
   *
   * @param validationMessagePatch the validation message to record
   * @param isClaim whether the message is for a claim or a submission
   */
  public void recordValidationMessage(
      ValidationMessagePatch validationMessagePatch, boolean isClaim) {
    String type = isClaim ? "Claim" : "Submission";
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

  private void incrementErrorType(String source, String type, String errorType) {
    errorTypeCounter.labelValues(source, type, errorType).inc();
  }

  /**
   * Starts a timer for a submission validation. If a timer was already started for this submission,
   * the old timer is removed.
   *
   * @param submissionId the ID of the submission to start the timer for
   */
  public void startSubmissionValidationTimer(UUID submissionId) {
    if (submissionValidationTimers.containsKey(submissionId)) {
      log.warn("Timer already started for submission {}, old timer will be removed", submissionId);
    }
    submissionValidationTimers.put(
        submissionId,
        new TimerLifecycle(
            this.submissionValidationTimeSummary.startTimer(), System.currentTimeMillis()));
  }

  /**
   * Stops a timer for a submission validation and records the timer value.
   *
   * @param claimId the ID of the claim the timer was started for.
   */
  public void stopSubmissionValidationTimer(UUID claimId) {
    TimerLifecycle timer = submissionValidationTimers.remove(claimId);
    if (!Objects.isNull(timer) && !Objects.isNull(timer.timer())) {
      double v = timer.timer().observeDuration();
      if (v > 2) {
        log.warn("Submission validation took {} seconds for claim {}", v, claimId);
      }
    }
  }

  /**
   * Starts a timer for a claim validation. If a timer was already started for this claim, the old
   * timer is removed.
   *
   * @param claimId the ID of the claim to start the timer for
   */
  public void startClaimValidationTimer(UUID claimId) {
    if (submissionValidationTimers.containsKey(claimId)) {
      log.warn("Timer already started for claim {}, old timer will be removed", claimId);
    }
    claimValidationTimers.put(
        claimId,
        new TimerLifecycle(
            this.claimValidationTimeSummary.startTimer(), System.currentTimeMillis()));
  }

  /**
   * Stops a timer for a claim validation and records the timer value.
   *
   * @param claimId the ID of the claim the timer was started for.
   */
  public void stopClaimValidationTimer(UUID claimId) {
    TimerLifecycle timer = claimValidationTimers.remove(claimId);
    if (!Objects.isNull(timer) && !Objects.isNull(timer.timer())) {
      double v = timer.timer().observeDuration();
      if (v > 2) {
        log.warn("Claim validation took {} seconds for claim {}", v, claimId);
      }
    }
  }

  /**
   * Starts a timer for a FSP validation. If a timer was already started for this claim, the old
   * timer is removed.
   *
   * @param claimId the ID of the claim to start the timer for
   */
  public void startFspValidationTimer(UUID claimId) {
    if (submissionValidationTimers.containsKey(claimId)) {
      log.warn("Timer already started for claim {}, old timer will be removed", claimId);
    }
    fspValidationTimers.put(
        claimId,
        new TimerLifecycle(
            this.fspValidationTimeSummary.startTimer(), System.currentTimeMillis()));
  }

  /**
   * Stops a timer for a FSP validation and records the timer value.
   *
   * @param claimId the ID of the claim the timer was started for.
   */
  public void stopFspValidationTimer(UUID claimId) {
    TimerLifecycle timer = fspValidationTimers.remove(claimId);
    if (!Objects.isNull(timer) && !Objects.isNull(timer.timer())) {
      double v = timer.timer().observeDuration();
      if (v > 2) {
        log.warn("FSP calculation and validation took {} seconds for claim {}", v, claimId);
      }
    }
  }

  /**
   * Removes all timers older than the specified number of minutes.
   *
   * @param minutes total minutes timer should be kept alive for
   */
  public void removeAllTimersOlderThanTotalMinutes(int minutes) {
    long currentTime = System.currentTimeMillis();
    for (TimerLifecycle timer : submissionValidationTimers.values()) {
      if (currentTime - timer.startTime() > (long) minutes * 60 * 1000) {
        timer.timer().close();
      }
    }
    for (TimerLifecycle timer : claimValidationTimers.values()) {
      if (currentTime - timer.startTime() > (long) minutes * 60 * 1000) {
        timer.timer().close();
      }
    }
    for (TimerLifecycle timer : fspValidationTimers.values()) {
      if (currentTime - timer.startTime() > (long) minutes * 60 * 1000) {
        timer.timer().close();
      }
    }
  }
}
