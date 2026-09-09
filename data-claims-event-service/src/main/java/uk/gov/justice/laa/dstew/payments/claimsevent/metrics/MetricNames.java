package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Short-name constants for all metrics published by this service, and the tag keys used to annotate
 * those metrics.
 *
 * <p>Metric names are the names <em>without</em> the namespace prefix. The fully-qualified
 * Prometheus metric name is always {@code namespace + shortName}. Use these constants rather than
 * raw strings so that renames are caught at compile time.
 *
 * <p>Tag key constants define the standard set of Micrometer tag keys used across the service.
 * Centralising them here ensures consistency — a tag key that drifts between call sites produces
 * separate Prometheus series that are impossible to correlate.
 *
 * <p>This class is intentionally separate from {@link MetricPublisher} — it represents <em>what
 * this application measures</em>, not <em>how metrics are published</em>. Keeping the two concerns
 * in separate files means {@link MetricPublisher} can be extracted to a shared library without
 * carrying application-specific metric names with it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MetricNames {

  // ---------------------------------------------------------------------------
  // Metric names
  // ---------------------------------------------------------------------------

  public static final String FILE_PARSING_TIME = "file_parsing_time";
  public static final String SUBMISSION_VALIDATION_TIME = "submission_validation_time";
  public static final String CLAIM_VALIDATION_TIME = "claim_validation_time";
  public static final String FSP_VALIDATION_TIME = "fsp_validation_time";
  public static final String SUBMISSIONS_ADDED = "submissions_added";
  public static final String CLAIMS_ADDED = "claims_added";
  public static final String SUBMISSIONS_WITH_ERRORS = "submissions_with_errors";
  public static final String CLAIMS_VALIDATED_VALID = "claims_validated_and_valid";
  public static final String CLAIMS_VALIDATED_WARNINGS = "claims_validated_and_warnings_found";
  public static final String CLAIMS_VALIDATED_INVALID = "claims_validated_and_invalid";
  public static final String VALID_SUBMISSIONS = "valid_submissions";
  public static final String INVALID_SUBMISSIONS = "invalid_submissions";
  public static final String MESSAGES_ERRORS = "messages_errors";
  public static final String MESSAGES_WARNINGS = "messages_warnings";

  /** Short name for the WebClient outbound HTTP request latency timer. */
  public static final String HTTP_CLIENT_REQUESTS = "http_client_requests";

  // ---------------------------------------------------------------------------
  // Tag keys
  // ---------------------------------------------------------------------------

  /** Tag identifying the source of a validation error (e.g. a validator class name). */
  public static final String TAG_ERROR_SOURCE = "error_source";

  /**
   * Tag identifying the type of entity being validated (e.g. {@code "Claim"}, {@code
   * "Submission"}).
   */
  public static final String TAG_TYPE = "type";

  /** Tag carrying the resolved validation message text. */
  public static final String TAG_MESSAGE = "message";

  /** Tag identifying the downstream API being called (e.g. {@code "data-claims-api"}). */
  public static final String TAG_API = "api";

  /** Tag carrying the HTTP method of an outbound request (e.g. {@code "GET"}, {@code "POST"}). */
  public static final String TAG_METHOD = "method";

  /**
   * Tag carrying the HTTP status code of a response, or {@code "CLIENT_ERROR"} if no response was
   * received.
   */
  public static final String TAG_STATUS = "status";
}
