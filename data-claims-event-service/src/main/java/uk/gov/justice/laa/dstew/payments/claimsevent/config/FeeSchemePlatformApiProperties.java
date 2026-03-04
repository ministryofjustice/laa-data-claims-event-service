package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpHeaders;

/**
 * Configuration properties for the Fee Scheme Platform API client.
 *
 * <p>Bound from the {@code laa.fee-scheme-platform-api} prefix. All timeout, pool, and buffer
 * values are sourced from {@code application.yml} (or environment-variable overrides) and default
 * to values appropriate for this API — see the YAML for documented defaults.
 */
@ConfigurationProperties(prefix = FeeSchemePlatformApiProperties.PREFIX)
public class FeeSchemePlatformApiProperties extends ApiProperties {

  /** The configuration prefix and logical name for this API client. */
  public static final String PREFIX = "laa.fee-scheme-platform-api";

  public FeeSchemePlatformApiProperties() {
    setAuthHeader(HttpHeaders.AUTHORIZATION);
    setName(PREFIX);
  }
}
