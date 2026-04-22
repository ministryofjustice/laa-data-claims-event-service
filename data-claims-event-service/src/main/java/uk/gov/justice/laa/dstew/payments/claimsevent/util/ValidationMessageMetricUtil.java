package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.MetricNames;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.MetricPublisher;

/**
 * Utility class that translates {@link ValidationMessagePatch} domain objects into generic metric
 * increments via {@link MetricPublisher}.
 *
 * <p>This is intentionally a stateless utility — all methods are static and the class is not
 * instantiable.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationMessageMetricUtil {

  /**
   * Records each {@link ValidationMessagePatch} in the supplied list as a labelled Micrometer
   * counter, tagged with {@code error_source}, {@code type}, and the resolved message text.
   *
   * <p>The mapping rules applied are:
   *
   * <ul>
   *   <li>ERROR messages → {@link MetricNames#MESSAGES_ERRORS}
   *   <li>All other types → {@link MetricNames#MESSAGES_WARNINGS}
   *   <li>{@code technicalMessage} is preferred over {@code displayMessage} when present
   * </ul>
   *
   * @param metricService the metric service to increment counters on
   * @param messages the validation messages to record
   * @param type a label identifying the source context, e.g. {@code "Claim"} or {@code
   *     "Submission"}
   */
  public static void incrementValidationMessageMetrics(
      MetricPublisher metricService, List<ValidationMessagePatch> messages, String type) {
    messages.forEach(
        message ->
            metricService.increment(
                resolveShortName(message),
                MetricNames.TAG_ERROR_SOURCE,
                message.getSource(),
                MetricNames.TAG_TYPE,
                type,
                MetricNames.TAG_MESSAGE,
                resolveText(message)));
  }

  /**
   * Resolves the metric short name for the given message.
   *
   * @param message the validation message
   * @return {@link MetricNames#MESSAGES_ERRORS} for ERROR type, {@link
   *     MetricNames#MESSAGES_WARNINGS} otherwise
   */
  private static String resolveShortName(ValidationMessagePatch message) {
    return ValidationMessageType.ERROR.equals(message.getType())
        ? MetricNames.MESSAGES_ERRORS
        : MetricNames.MESSAGES_WARNINGS;
  }

  /**
   * Resolves the display text for the given message, preferring {@code technicalMessage} over
   * {@code displayMessage} when present.
   *
   * @param message the validation message
   * @return the resolved message text
   */
  private static String resolveText(ValidationMessagePatch message) {
    return StringUtils.hasText(message.getTechnicalMessage())
        ? message.getTechnicalMessage()
        : message.getDisplayMessage();
  }
}
