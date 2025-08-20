package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties specific to the Provider Details API.
 *
 * @author Jamie Briggs
 */
@ConfigurationProperties(prefix = "laa.claims-api")
public class DataClaimsApiProperties extends ApiProperties {

  public DataClaimsApiProperties(String url, String host, int port, String accessToken) {
    super(url, host, port, accessToken);
  }
}
