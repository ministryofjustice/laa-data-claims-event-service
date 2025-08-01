package uk.gov.justice.laa.bulk.claim.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import uk.gov.justice.laa.bulk.claim.data.client.http.ClaimsApiClient;
import uk.gov.justice.laa.bulk.claim.service.ProviderDetailsRestService;

/**
 * Configuration class for creating and configuring WebClient instances.
 *
 * @author Jamie Briggs
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({ProviderDetailsApiProperties.class, ClaimsApiProperties.class})
public class WebClientConfiguration {

  /**
   * Creates a {@link ProviderDetailsRestService} bean to communicate with the Provider Details API
   * using a WebClient instance.
   *
   * @param properties The configuration properties required to initialize the WebClient, including
   *     the base URL and access token for the Provider Details API.
   * @return An instance of {@link ProviderDetailsRestService} for interacting with the Provider
   *     Details API.
   */
  @Bean
  public ProviderDetailsRestService providerDetailsService(
      final ProviderDetailsApiProperties properties) {
    final WebClient webClient = createWebClient(properties);
    final WebClientAdapter webClientAdapter = WebClientAdapter.create(webClient);
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(webClientAdapter).build();

    return factory.createClient(ProviderDetailsRestService.class);
  }

  /**
   * Creates a {@link ClaimsApiClient} bean to communicate with the Claims API using a WebClient
   * instance.
   *
   * @param properties The configuration properties required to initialize the WebClient, including
   *     the base URL and access token for the Provider Details API.
   * @return An instance of {@link ClaimsApiClient} for interacting with the Claims API.
   */
  @Bean
  public ClaimsApiClient claimsApiClient(final ClaimsApiProperties properties) {
    final WebClient webClient = createWebClient(properties);
    return new ClaimsApiClient(webClient);
  }

  /**
   * Creates a WebClient instance using the provided configuration properties.
   *
   * @param apiProperties The configuration properties for the API.
   * @return A WebClient instance.
   */
  public static WebClient createWebClient(final ApiProperties apiProperties) {
    final ExchangeStrategies strategies =
        ExchangeStrategies.builder().codecs(ClientCodecConfigurer::defaultCodecs).build();
    return WebClient.builder()
        .baseUrl(apiProperties.getUrl())
        .defaultHeader(HttpHeaders.AUTHORIZATION, apiProperties.getAccessToken())
        .exchangeStrategies(strategies)
        .build();
  }
}
