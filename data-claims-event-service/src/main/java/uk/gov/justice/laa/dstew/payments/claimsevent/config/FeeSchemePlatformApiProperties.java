package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties specific to the Fee Scheme Platform API. */
@ConfigurationProperties(prefix = "laa.fee-scheme-platform-api")
public class FeeSchemePlatformApiProperties extends ApiProperties {

  public FeeSchemePlatformApiProperties(String url, String host, int port, String accessToken) {
    super(url, host, port, accessToken);
  }
}
