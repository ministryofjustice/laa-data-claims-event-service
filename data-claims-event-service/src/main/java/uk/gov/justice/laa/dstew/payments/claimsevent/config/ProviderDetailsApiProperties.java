package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties specific to the Provider Details API.
 *
 * @author Jamie Briggs
 */
@ConfigurationProperties(prefix = "laa.provider-details-api")
public class ProviderDetailsApiProperties extends ApiProperties {

  public ProviderDetailsApiProperties(String url, String host, int port, String accessToken) {
    super(url, host, port, accessToken);
  }
}
