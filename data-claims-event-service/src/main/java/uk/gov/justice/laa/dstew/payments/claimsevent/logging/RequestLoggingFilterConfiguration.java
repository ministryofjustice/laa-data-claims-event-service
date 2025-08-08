package uk.gov.justice.laa.dstew.payments.claimsevent.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Configuration class for request logging filter. This filter logs incoming requests, including
 * query strings and payloads.
 */
@Configuration
public class RequestLoggingFilterConfiguration {

  /**
   * Creates a {@link CommonsRequestLoggingFilter} bean. This filter logs request details such as
   * query strings and payloads.
   *
   * @return the configured request logging filter
   */
  @Bean
  public CommonsRequestLoggingFilter logFilter() {
    CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
    filter.setIncludeQueryString(true);
    filter.setIncludePayload(true);
    filter.setMaxPayloadLength(50_000);
    return filter;
  }
}
