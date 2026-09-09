package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration for Pact tests. Handles creating beans which otherwise would not be
 * automatically created.
 */
@TestConfiguration
public class ClaimsApiPactTestConfig {

  @Bean
  MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
  }
}
