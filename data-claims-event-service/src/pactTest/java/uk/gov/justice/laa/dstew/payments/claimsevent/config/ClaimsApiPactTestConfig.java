package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.Collections;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.config.DataClaimsApiConfig;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.config.FeeSchemeApiConfig;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.config.ProviderDetailsApiConfig;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.validator.claim.ClaimValidation;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.validator.submission.SubmissionValidation;

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

  @Bean
  public ClaimValidation coreClaimValidation() {
    return new ClaimValidation(Collections.emptyList());
  }

  @Bean
  public SubmissionValidation coreSubmissionValidation() {
    return new SubmissionValidation(Collections.emptyList());
  }

  @Bean
  @Primary
  public DataClaimsApiConfig coreDataClaimsApiConfig() {
    DataClaimsApiConfig cfg = new DataClaimsApiConfig();
    cfg.setUrl("http://localhost:30000");
    cfg.setAccessToken("");
    return cfg;
  }

  @Bean
  @Primary
  public FeeSchemeApiConfig coreFeeSchemeApiConfig() {
    FeeSchemeApiConfig cfg = new FeeSchemeApiConfig();
    cfg.setUrl("http://localhost:30000");
    cfg.setAccessToken("");
    return cfg;
  }

  @Bean
  @Primary
  public ProviderDetailsApiConfig coreProviderDetailsApiConfig() {
    ProviderDetailsApiConfig cfg = new ProviderDetailsApiConfig();
    cfg.setUrl("http://localhost:30000");
    cfg.setAccessToken("");
    return cfg;
  }
}
