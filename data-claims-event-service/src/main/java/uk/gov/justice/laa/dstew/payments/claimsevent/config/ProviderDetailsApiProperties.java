package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Provider Details API client.
 *
 * <p>Bound from the {@code laa.provider-details-api} prefix. All timeout, pool, and buffer values
 * are sourced from {@code application.yml} (or environment-variable overrides) and default to
 * values appropriate for this API — see the YAML for documented defaults.
 */
@ConfigurationProperties(prefix = ProviderDetailsApiProperties.PREFIX)
public class ProviderDetailsApiProperties extends ApiProperties {

  /** The configuration prefix and logical name for this API client. */
  public static final String PREFIX = "laa.provider-details-api";

  public ProviderDetailsApiProperties() {
    setAuthHeader("X-Authorization");
    setName(PREFIX);
  }
}
