package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Package-private utilities for validating and transforming metric names and tag arrays.
 *
 * <p>All methods are stateless and shared across {@link MetricPublisher} and {@link MetricSample}
 * to ensure consistent behaviour regardless of which entry point is used.
 *
 * <p>This class is intentionally package-private — it is an internal implementation detail of the
 * metrics package and is not part of the public API.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MetricUtils {

  /**
   * Validates that {@code tags} is an even-length array containing no {@code null} elements.
   *
   * <p>An odd-length array indicates an unpaired key — a programmer error that should fail
   * immediately rather than silently producing malformed metrics. A {@code null} key or value is
   * equally a programmer error and is rejected at the point of entry rather than being silently
   * dropped downstream.
   *
   * @param label a descriptive name for the array used in exception messages (e.g. {@code "tags"},
   *     {@code "startTags"}, {@code "stopTags"})
   * @param tags the flat key/value array to validate
   * @throws IllegalArgumentException if {@code tags} has an odd number of elements
   * @throws NullPointerException if any element within {@code tags} is {@code null}
   */
  static void validateTags(String label, String[] tags) {
    if (tags.length % 2 != 0) {
      throw new IllegalArgumentException(
          label + " must contain an even number of elements but got: " + tags.length);
    }
    for (int i = 0; i < tags.length; i++) {
      Objects.requireNonNull(tags[i], label + " element at index " + i + " must not be null");
    }
  }

  /**
   * Converts a snake_case short metric name into a title-cased, space-separated label suitable for
   * use as a human-readable metric description.
   *
   * <p>For example, {@code "operation_duration"} becomes {@code "Operation Duration"}.
   *
   * <p>Note: a leading underscore (e.g. {@code "_operation_duration"}) is silently dropped — the
   * guard that suppresses the leading space also suppresses the underscore itself. This is
   * intentional: metric short names should never start with an underscore.
   *
   * @param shortName the snake_case metric name to convert; {@code null} and blank values are
   *     returned as-is
   * @return the title-cased label, or the original value if {@code null} or blank
   */
  static String toLabel(String shortName) {
    if (shortName == null || shortName.isBlank()) {
      return shortName;
    }
    StringBuilder sb = new StringBuilder(shortName.length());
    boolean capitaliseNext = true;
    for (int i = 0; i < shortName.length(); i++) {
      char c = shortName.charAt(i);
      if (c == '_') {
        if (!sb.isEmpty()) {
          sb.append(' ');
        }
        capitaliseNext = true;
      } else if (capitaliseNext) {
        sb.append(Character.toUpperCase(c));
        capitaliseNext = false;
      } else {
        sb.append(Character.toLowerCase(c));
      }
    }
    return sb.toString();
  }
}
