package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpHeaders;

/**
 * Configuration properties for the Data Claims API client.
 *
 * <p>Bound from the {@code laa.claims-api} prefix. All timeout, pool, and buffer values are sourced
 * from {@code application.yml} (or environment-variable overrides) and default to values
 * appropriate for a bulk-submission API — see the YAML for documented defaults.
 */
@ConfigurationProperties(prefix = DataClaimsApiProperties.PREFIX)
public class DataClaimsApiProperties extends ApiProperties {

  /** The configuration prefix and logical name for this API client. */
  public static final String PREFIX = "laa.claims-api";

  public DataClaimsApiProperties() {
    setAuthHeader(HttpHeaders.AUTHORIZATION);
    setName(PREFIX);
  }
}
