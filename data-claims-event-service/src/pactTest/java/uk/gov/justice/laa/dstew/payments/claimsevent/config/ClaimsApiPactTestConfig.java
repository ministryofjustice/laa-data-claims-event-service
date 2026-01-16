package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration for Pact tests. Handles creating beans which otherwise would not be
 * automatically created.
 */
@TestConfiguration
public class ClaimsApiPactTestConfig {

  @Bean
  PrometheusRegistry prometheusRegistry() {
    return Mockito.mock(PrometheusRegistry.class);
  }
}
