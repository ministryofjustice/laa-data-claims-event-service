package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import io.prometheus.metrics.core.datapoints.Timer;

/**
 * Wrapper record to store a Timer and it's start time. This helps with removing stale timers.
 *
 * @param timer the Histogram {@link Timer}.
 * @param startTime the start time of the Timer.
 * @author Jamie Briggs
 */
record TimerLifecycle(Timer timer, long startTime) {}
