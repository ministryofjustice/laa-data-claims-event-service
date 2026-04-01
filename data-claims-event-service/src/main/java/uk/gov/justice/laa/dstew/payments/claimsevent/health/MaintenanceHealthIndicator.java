package uk.gov.justice.laa.dstew.payments.claimsevent.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.ShutdownService;

/** Health indicator that reports DOWN when the application is in maintenance/drain mode. */
@Component("maintenance")
public class MaintenanceHealthIndicator implements HealthIndicator {

  private final ShutdownService shutdownService;

  public MaintenanceHealthIndicator(ShutdownService shutdownService) {
    this.shutdownService = shutdownService;
  }

  @Override
  public Health health() {
    if (!shutdownService.isAcceptingWork()) {
      return Health.down().withDetail("inFlightCount", shutdownService.getInFlightCount()).build();
    }
    return Health.up().build();
  }
}
