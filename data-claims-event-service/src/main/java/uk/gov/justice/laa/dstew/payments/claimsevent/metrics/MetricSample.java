package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An opaque handle representing an in-progress timer measurement, bound to a specific metric name
 * and a set of tags known at the point the timer was started.
 *
 * <p>Instances are created exclusively by {@link MetricPublisher#startTimer(String, String...)} and
 * should be stopped by calling {@link #stop(String...)} when the operation completes. This is the
 * async counterpart to the synchronous try-with-resources pattern provided by {@link MetricTimer}.
 *
 * <p>Additional tags discovered during execution (e.g. an HTTP status code) can be supplied at stop
 * time and are merged with the start-time tags. Stop-time tags take precedence over start-time tags
 * when the same key appears in both:
 *
 * <pre>{@code
 * TimerSample sample = publisher.startTimer("http_client_requests",
 *     "api", "my-api", "method", "GET", "uri", "/items");
 * // ... async work ...
 * sample.stop("status", "200");
 * }</pre>
 *
 * <p>If all context is known at start time, call {@link #stop(String...)} with no arguments:
 *
 * <pre>{@code
 * TimerSample sample = publisher.startTimer("operation_duration", "region", "eu-west");
 * // ... async work ...
 * sample.stop();
 * }</pre>
 *
 * <p>This class is intentionally package-private in construction — only {@link MetricPublisher} may
 * create instances, ensuring the registry and namespace are always controlled centrally.
 * Internally, {@link MetricSample#stop(String...)} calls the package-private {@link
 * MetricPublisher#buildTimer(String, String...)} to resolve the correct timer at stop time.
 */
public final class MetricSample {

  private final Timer.Sample sample;
  private final MetricPublisher publisher;
  private final String shortName;
  private final String[] startTags;

  /**
   * Package-private constructor — instances are created only by {@link MetricPublisher}.
   *
   * @param sample the underlying Micrometer timer sample, already started
   * @param publisher the publisher used to resolve and register the timer on stop
   * @param shortName the short metric name (namespace will be prepended by the publisher)
   * @param startTags key/value tag pairs known at start time
   */
  MetricSample(
      Timer.Sample sample, MetricPublisher publisher, String shortName, String[] startTags) {
    this.sample = sample;
    this.publisher = publisher;
    this.shortName = shortName;
    this.startTags = startTags;
  }

  /**
   * Stops this sample and records the elapsed duration, merging any additional tags with those
   * supplied at start time.
   *
   * <p>Stop-time tags take precedence — if the same key appears in both the start-time and
   * stop-time arrays, the stop-time value is used.
   *
   * @param stopTags optional additional key/value tag pairs discovered during execution; must be an
   *     even number of non-null elements
   * @throws IllegalArgumentException if {@code stopTags} contains an odd number of elements
   */
  public void stop(String... stopTags) {
    String[] merged = merge(startTags, stopTags);
    sample.stop(publisher.buildTimer(shortName, merged));
  }

  /**
   * Merges start-time and stop-time tag arrays into a single flat array.
   *
   * <p>Stop-time tags take precedence — if the same key appears in both arrays, the stop-time value
   * overwrites the start-time value. Ordering in the result is: start-time tags first (minus any
   * overridden keys), then stop-time tags.
   *
   * @param startTags tags supplied at timer start
   * @param stopTags tags supplied at timer stop
   * @return a merged flat key/value array
   */
  private static String[] merge(String[] startTags, String[] stopTags) {
    MetricUtils.validateTags("startTags", startTags);
    MetricUtils.validateTags("stopTags", stopTags);
    if (stopTags.length == 0) {
      return startTags;
    }
    if (startTags.length == 0) {
      return stopTags;
    }
    // Collect stop-tag keys so we can skip any start-tag entry with the same key.
    // Validation above guarantees no nulls so no null-guard is needed here.
    Set<String> stopKeys = new LinkedHashSet<>();
    for (int i = 0; i < stopTags.length; i += 2) {
      stopKeys.add(stopTags[i]);
    }
    // Build result: start tags (excluding overridden keys) + stop tags
    List<String> result = new ArrayList<>(startTags.length + stopTags.length);
    for (int i = 0; i < startTags.length; i += 2) {
      if (!stopKeys.contains(startTags[i])) {
        result.add(startTags[i]);
        result.add(startTags[i + 1]);
      }
    }
    Collections.addAll(result, stopTags);
    return result.toArray(new String[0]);
  }
}
