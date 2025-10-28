package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;

/**
 * Configuration class for scheduling metrics removal.
 *
 * @author Jamie Briggs
 */
@Configuration
@EnableScheduling
public class MetricsConfiguration {

  private final EventServiceMetricService eventServiceMetricService;

  public MetricsConfiguration(EventServiceMetricService eventServiceMetricService) {
    this.eventServiceMetricService = eventServiceMetricService;
  }

  /** Removes all timers older than a specified amount of minutes. */
  @Scheduled(cron = "0 0 * * * *")
  public void removeStaleTimers() {
    eventServiceMetricService.removeAllTimersOlderThanTotalMinutes(60);
  }
}
