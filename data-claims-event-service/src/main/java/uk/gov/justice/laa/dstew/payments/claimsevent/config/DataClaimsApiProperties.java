package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpHeaders;

/**
 * Configuration properties specific to the Provider Details API.
 *
 * @author Jamie Briggs
 */
@ConfigurationProperties(prefix = "laa.claims-api")
public class DataClaimsApiProperties extends ApiProperties {

  public DataClaimsApiProperties(String url, String accessToken) {
    super(url, accessToken, HttpHeaders.AUTHORIZATION);
  }
}
