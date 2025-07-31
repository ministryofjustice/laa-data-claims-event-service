package uk.gov.justice.laa.bulk.claim.data.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.justice.laa.bulk.claim.data.client.http.BulkClaimsSubmissionApiClient;
import uk.gov.justice.laa.bulk.claim.data.client.http.ClaimsApiClient;

/** Claims API client configuration. */
@Configuration
public class WebClientConfig {

  @Value("${laa.claims-api.base-url}")
  private String baseUrl;

  @Bean
  public BulkClaimsSubmissionApiClient bulkSubmissionClient() {
    return new ClaimsApiClient(baseUrl);
  }
}
