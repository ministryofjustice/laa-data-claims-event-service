package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * An {@link AutoCloseable} handle wrapping a Micrometer {@link Timer.Sample} for a single timed
 * operation.
 *
 * <p>Returned by each {@code time*()} factory method on {@link MetricPublisher} and intended for
 * use in a try-with-resources block, which guarantees the timer is stopped and recorded even if the
 * enclosed operation throws an exception:
 *
 * <pre>{@code
 * try (var ignored = metricService.timer(MetricNames.CLAIM_VALIDATION_TIME, claimId)) {
 *     // ... timed operation ...
 * }
 * }</pre>
 *
 * <p>If the elapsed duration exceeds the configured warn threshold, a {@code WARN} log entry is
 * emitted using the message supplied by {@code warnMessageSupplier}. The supplier is only invoked
 * when the threshold is actually breached, avoiding unnecessary string construction on the happy
 * path.
 *
 * <p>Elapsed time is measured using the Micrometer {@link Clock} sourced from the registry, rather
 * than {@link java.time.Instant#now()}. This ensures the warn threshold calculation uses the same
 * clock as the {@link Timer.Sample}, making behaviour consistent and the clock substitutable in
 * tests via a mock registry.
 */
@Slf4j
public final class MetricTimer implements AutoCloseable {

  private static final double NANOS_PER_SECOND = 1_000_000_000.0;

  private final Timer.Sample sample;
  private final Timer timer;
  private final double warnThresholdSeconds;
  private final Supplier<String> warnMessageSupplier;
  private final Clock clock;
  private final long startNanos;

  /**
   * Constructs a {@link MetricTimer} with an already-started Micrometer {@link Timer.Sample}.
   *
   * @param sample the started Micrometer sample to stop on close
   * @param timer the {@link Timer} to record the sample against
   * @param warnThresholdSeconds elapsed duration in seconds above which a {@code WARN} is logged
   * @param warnMessageSupplier lazily evaluated message used in the warn log; only called when the
   *     threshold is exceeded
   * @param clock the Micrometer {@link Clock} used to measure elapsed time — sourced from the
   *     registry so tests can substitute a fixed clock
   */
  MetricTimer(
      Timer.Sample sample,
      Timer timer,
      double warnThresholdSeconds,
      Supplier<String> warnMessageSupplier,
      Clock clock) {
    this.sample = sample;
    this.timer = timer;
    this.warnThresholdSeconds = warnThresholdSeconds;
    this.warnMessageSupplier = warnMessageSupplier;
    this.clock = clock;
    this.startNanos = clock.monotonicTime();
  }

  /**
   * Stops the timer, records the observation to the Micrometer registry, and emits a warning log if
   * the elapsed duration exceeds the configured threshold.
   */
  @Override
  public void close() {
    sample.stop(timer);
    double elapsed = (clock.monotonicTime() - startNanos) / NANOS_PER_SECOND;
    if (elapsed > warnThresholdSeconds) {
      log.warn("{} took {} seconds", warnMessageSupplier.get(), elapsed);
    }
  }
}
