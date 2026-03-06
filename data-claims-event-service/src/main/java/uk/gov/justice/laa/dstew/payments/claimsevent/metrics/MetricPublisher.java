package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.MetricsProperties;

/**
 * Generic Micrometer-backed publisher for counters and timers.
 *
 * <p>Counters track discrete events via {@link #increment}. Timers are obtained via {@link #timer}
 * and are intended for use in try-with-resources blocks, guaranteeing the sample is recorded even
 * when the enclosed operation throws an exception.
 *
 * <p>All metric names are automatically prefixed with the namespace supplied at construction time.
 * Callers should reference short-name constants rather than raw strings so that renames are caught
 * at compile time. The fully-qualified Prometheus metric name is always {@code namespace +
 * shortName}.
 *
 * <p>Metrics are published through the Micrometer {@link MeterRegistry} and exposed at {@code
 * /actuator/prometheus} for collection by Prometheus.
 */
@Slf4j
@Component
public class MetricPublisher {

  /**
   * Percentiles published for every timer registered by this publisher.
   *
   * <p>Produces Prometheus histogram buckets at the 50th, 90th, 95th, and 99th percentiles, giving
   * visibility into both typical latency (p50) and tail latency (p99). Adjust here to change
   * percentile coverage across all timers in the service simultaneously.
   */
  private static final double[] TIMER_PERCENTILES = {0.5, 0.9, 0.95, 0.99};

  private final MeterRegistry meterRegistry;
  private final String namespace;
  private final double warnThresholdSeconds;

  /**
   * Constructs the publisher with the supplied registry and configuration.
   *
   * <p>Counters and timers are created on first use — Micrometer's builders are idempotent, so
   * subsequent calls with the same name and tags return the existing meter rather than creating a
   * new one.
   *
   * @param meterRegistry the Micrometer registry to publish metrics to
   * @param metricsProperties externalised configuration supplying the metric namespace prefix and
   *     the slow-operation warn threshold
   * @throws IllegalArgumentException if {@code namespace} is null or blank, or if {@code
   *     warnThresholdSeconds} is zero or negative
   */
  public MetricPublisher(MeterRegistry meterRegistry, MetricsProperties metricsProperties) {
    String ns = metricsProperties.getNamespace();
    double threshold = metricsProperties.getWarnThresholdSeconds();
    if (ns == null || ns.isBlank()) {
      throw new IllegalArgumentException("MetricPublisher: namespace must not be null or blank");
    }
    if (threshold <= 0) {
      throw new IllegalArgumentException(
          "MetricPublisher: warnThresholdSeconds must be positive, got: " + threshold);
    }
    this.meterRegistry = meterRegistry;
    this.namespace = ns;
    this.warnThresholdSeconds = threshold;
    log.info(
        "MetricPublisher initialised — namespace='{}', warnThresholdSeconds={}",
        this.namespace,
        this.warnThresholdSeconds);
  }

  /**
   * Starts a timer measurement for the named operation and returns an opaque {@link MetricSample}
   * handle.
   *
   * <p>This is the async counterpart to {@link #timer(String, UUID, String...)} for use in reactive
   * or callback-based contexts where a try-with-resources block cannot be used.
   *
   * <p>Tags known at the start of the operation are bound here. Additional tags discovered during
   * execution (e.g. an HTTP status code) can be supplied when calling {@link
   * MetricSample#stop(String...)}, where they are merged with the start-time tags. Stop-time tags
   * take precedence on key conflict:
   *
   * <pre>{@code
   * TimerSample sample = publisher.startTimer("http_client_requests",
   *     "api", "my-api", "method", "GET", "uri", "/items");
   * // ... async work ...
   * sample.stop("status", "200");
   * }</pre>
   *
   * <p>If all context is known upfront, call {@link MetricSample#stop(String...)} with no
   * arguments:
   *
   * <pre>{@code
   * TimerSample sample = publisher.startTimer("operation_duration", "region", "eu-west");
   * sample.stop();
   * }</pre>
   *
   * <p>The caller has no knowledge of the underlying Micrometer types — the registry, namespace,
   * and timer configuration are all managed by this publisher.
   *
   * @param shortName short metric name (namespace is prepended automatically)
   * @param startTags optional key/value tag pairs known at start time — must be an even number of
   *     non-null elements
   * @return a started {@link MetricSample} ready to be stopped when the operation completes
   * @throws IllegalArgumentException if {@code startTags} contains an odd number of elements
   */
  public MetricSample startTimer(String shortName, String... startTags) {
    return new MetricSample(Timer.start(meterRegistry), this, shortName, startTags);
  }

  /**
   * Increments the named counter by 1, registering it on first use.
   *
   * <p>The configured namespace is automatically prepended to {@code shortName}. Optional tags are
   * supplied as flat key/value pairs:
   *
   * <pre>{@code
   * // No tags
   * publisher.increment("events_processed");
   *
   * // With tags
   * publisher.increment("events_processed", "type", "A", "region", "eu-west");
   * }</pre>
   *
   * <p>Micrometer's {@link Counter#builder(String)} is idempotent — if a counter with the resolved
   * name and tags is already registered it is returned as-is, making repeated or concurrent calls
   * safe.
   *
   * @param shortName the short metric name (namespace is prepended automatically)
   * @param tags optional key/value tag pairs — must be an even number of non-null elements
   * @throws IllegalArgumentException if {@code tags} contains an odd number of elements
   */
  public void increment(String shortName, String... tags) {
    Counter.Builder builder = Counter.builder(namespace + shortName);
    toTagMap(tags).forEach(builder::tag);
    builder.register(meterRegistry).increment();
  }

  /**
   * Builds, registers, and returns a Micrometer {@link Timer} for the named operation.
   *
   * <p><strong>Visibility: package-private — intentional and permanent.</strong>
   *
   * <p>This method is the single source of truth for timer construction within this package. It is
   * shared by both {@link #timer(String, UUID, String...)} (synchronous) and {@link
   * MetricSample#stop(String...)} (async), ensuring that namespace prefix, description, and
   * percentile configuration are applied consistently regardless of which path is taken.
   *
   * <p>It is deliberately not {@code public} because exposing a raw {@link Timer} to external
   * callers would bypass the abstractions this class provides — callers would need direct knowledge
   * of Micrometer's {@link Timer.Sample} lifecycle and the registry, undermining the goal of
   * keeping metric instrumentation knowledge out of business code.
   *
   * <p>It is deliberately not {@code private} because {@link MetricSample} (same package) needs to
   * call it at stop time to resolve the correct timer for the merged tag set. Making it {@code
   * protected} would unnecessarily widen visibility to subclasses, which is not the intent — there
   * are no designed subclasses of {@link MetricPublisher}.
   *
   * <p>Micrometer's registry is idempotent — repeated calls with the same name and tags return the
   * same {@link Timer} instance rather than registering a new one.
   *
   * @param shortName short metric name (namespace is prepended automatically)
   * @param tags optional key/value tag pairs — must be an even number of non-null elements
   * @return the registered {@link Timer}
   * @throws IllegalArgumentException if {@code tags} contains an odd number of elements
   */
  Timer buildTimer(String shortName, String... tags) {
    Timer.Builder builder =
        Timer.builder(namespace + shortName)
            .description(MetricUtils.toLabel(shortName))
            .publishPercentiles(TIMER_PERCENTILES);
    toTagMap(tags).forEach(builder::tag);
    return builder.register(meterRegistry);
  }

  /**
   * Returns a started {@link MetricTimer} for the named operation.
   *
   * <p>A human-readable label is derived automatically from {@code shortName} — underscores are
   * replaced with spaces and each word is title-cased. For example, {@code "operation_duration"}
   * becomes {@code "Operation Duration"}.
   *
   * <p>Use in a try-with-resources block to guarantee the sample is always recorded:
   *
   * <pre>{@code
   * try (var ignored = publisher.timer("operation_duration", operationId)) {
   *     performOperation();
   * }
   * }</pre>
   *
   * @param shortName short metric name (namespace is prepended automatically)
   * @param id identifier for the timed operation, included in slow-operation log messages
   * @param tags optional key/value tag pairs — must be an even number of non-null elements
   * @return a started {@link MetricTimer} that records its sample on {@link MetricTimer#close()}
   * @throws IllegalArgumentException if {@code tags} contains an odd number of elements
   */
  public MetricTimer timer(String shortName, UUID id, String... tags) {
    String label = MetricUtils.toLabel(shortName);
    Timer timer = buildTimer(shortName, tags);
    return new MetricTimer(
        Timer.start(meterRegistry),
        timer,
        warnThresholdSeconds,
        () -> label + " id=" + id,
        meterRegistry.config().clock());
  }

  /**
   * Validates and converts a flat key/value tag array into an ordered {@link Map}.
   *
   * <p>Validation is delegated to {@link MetricUtils#validateTags} — odd-length arrays and {@code
   * null} elements are rejected immediately with a clear exception rather than being silently
   * skipped, ensuring malformed tag sets are caught at the call site.
   *
   * @param tags flat key/value pairs (e.g. {@code "region", "eu-west"})
   * @return an ordered map of tag pairs ready to apply to any Micrometer builder
   * @throws IllegalArgumentException if {@code tags} has an odd number of elements
   * @throws NullPointerException if any element within {@code tags} is {@code null}
   */
  private Map<String, String> toTagMap(String... tags) {
    MetricUtils.validateTags("tags", tags);
    // Simple for loop — avoids stream and lambda allocation for the common case
    // of a small number of tag pairs.
    Map<String, String> map = new LinkedHashMap<>(tags.length / 2);
    for (int i = 0; i < tags.length; i += 2) {
      map.put(tags[i], tags[i + 1]);
    }
    return map;
  }
}
