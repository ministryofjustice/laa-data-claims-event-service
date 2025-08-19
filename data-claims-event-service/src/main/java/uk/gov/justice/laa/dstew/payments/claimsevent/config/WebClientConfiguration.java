package uk.gov.justice.laa.dstew.payments.claimsevent.config;

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
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;

/**
 * Configuration class for creating and configuring WebClient instances.
 *
 * @author Jamie Briggs
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({
  ProviderDetailsApiProperties.class,
  ClaimsApiProperties.class,
  FeeSchemePlatformApiProperties.class
})
public class WebClientConfiguration {

  /**
   * Creates a {@link ProviderDetailsRestClient} bean to communicate with the Provider Details API
   * using a WebClient instance.
   *
   * @param properties The configuration properties required to initialize the WebClient, including
   *     the base URL and access token for the Provider Details API.
   * @return An instance of {@link ProviderDetailsRestClient} for interacting with the Provider
   *     Details API.
   */
  @Bean
  public ProviderDetailsRestClient providerDetailsService(
      final ProviderDetailsApiProperties properties) {
    final WebClient webClient = createWebClient(properties);
    final WebClientAdapter webClientAdapter = WebClientAdapter.create(webClient);
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(webClientAdapter).build();

    return factory.createClient(ProviderDetailsRestClient.class);
  }

  /**
   * Creates a {@link ClaimsRestClient} bean to communicate with the Claims API using a WebClient
   * instance.
   *
   * @param properties The configuration properties required to initialize the WebClient, including
   *     the base URL and access token for the Provider Details API.
   * @return An instance of {@link ClaimsRestClient} for interacting with the Claims API.
   */
  @Bean
  public ClaimsRestClient claimsApiClient(final ClaimsApiProperties properties) {
    final WebClient webClient = createWebClient(properties);
    return new ClaimsRestClient(webClient);
  }

  /**
   * Creates a {@link FeeSchemePlatformRestClient} bean to communicate with the Fee Scheme Platform
   * API using a WebClient instance.
   *
   * @param properties The configuration properties required to initialize the WebClient, including
   *     the base URL and access token for the Fee Scheme Platform API.
   * @return An instance of {@link FeeSchemePlatformRestClient} for interacting with the Fee Scheme
   *     Platform API.
   */
  @Bean
  public FeeSchemePlatformRestClient feeSchemePlatformRestService(
      final FeeSchemePlatformApiProperties properties) {
    final WebClient webClient = createWebClient(properties);
    final WebClientAdapter webClientAdapter = WebClientAdapter.create(webClient);
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(webClientAdapter).build();

    return factory.createClient(FeeSchemePlatformRestClient.class);
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
