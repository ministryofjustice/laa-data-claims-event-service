package uk.gov.justice.laa.bulk.claim.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for creating and configuring WebClient instances.
 *
 * @author Jamie Briggs
 */
@Configuration
@EnableConfigurationProperties({ProviderDetailsApiProperties.class})
public class WebClientConfiguration {

  @Bean("providerDetailsWebClient")
  WebClient providerDetailsWebClient(
      ProviderDetailsApiProperties properties, WebClient.Builder builder) {
    return createWebClient(properties, builder);
  }

  private WebClient createWebClient(final ApiProperties apiProperties, WebClient.Builder builder) {
    final int size = 16 * 1024 * 1024;
    final ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
            .build();
    return builder
        .baseUrl(apiProperties.getUrl())
        .defaultHeader(HttpHeaders.AUTHORIZATION, apiProperties.getAccessToken())
        .exchangeStrategies(strategies)
        .build();
  }
}
