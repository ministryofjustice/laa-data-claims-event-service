package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.WebClientLoggingFilter;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.WebClientMetricsFilter;

/**
 * Configuration class for creating and configuring {@link WebClient} instances for each downstream
 * REST API.
 *
 * <p>Each client is independently configured with:
 *
 * <ul>
 *   <li>A dedicated Reactor Netty connection pool (named for observability in Prometheus).
 *   <li>Per-API TCP connect, response, and pending-acquire timeouts sourced from {@link
 *       ApiProperties}.
 *   <li>Structured request/response logging via {@link WebClientLoggingFilter}.
 *   <li>Micrometer latency metrics via {@link WebClientMetricsFilter}.
 *   <li>An in-memory response-body buffer sized appropriately for each API's payload profile.
 * </ul>
 *
 * <p>All timeout, pool, and buffer values are externalised to {@code application.yml} and can be
 * overridden per environment.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({
  ProviderDetailsApiProperties.class,
  DataClaimsApiProperties.class,
  FeeSchemePlatformApiProperties.class
})
public class WebClientConfiguration {

  private final MeterRegistry meterRegistry;

  public WebClientConfiguration(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /**
   * Creates a {@link ProviderDetailsRestClient} bean backed by a WebClient configured for the
   * Provider Details API.
   *
   * @param properties API-specific configuration (URL, credentials, timeouts, pool size)
   * @return a strongly-typed HTTP client for the Provider Details API
   */
  @Bean
  public ProviderDetailsRestClient providerDetailsClient(
      final ProviderDetailsApiProperties properties) {
    return createClient(properties, ProviderDetailsRestClient.class);
  }

  /**
   * Creates a {@link DataClaimsRestClient} bean backed by a WebClient configured for the Data
   * Claims API.
   *
   * @param properties API-specific configuration (URL, credentials, timeouts, pool size, buffer)
   * @return a strongly-typed HTTP client for the Data Claims API
   */
  @Bean
  public DataClaimsRestClient claimsApiClient(final DataClaimsApiProperties properties) {
    return createClient(properties, DataClaimsRestClient.class);
  }

  /**
   * Creates a {@link FeeSchemePlatformRestClient} bean backed by a WebClient configured for the Fee
   * Scheme Platform API.
   *
   * @param properties API-specific configuration (URL, credentials, timeouts, pool size)
   * @return a strongly-typed HTTP client for the Fee Scheme Platform API
   */
  @Bean
  public FeeSchemePlatformRestClient feeSchemePlatformRestClient(
      final FeeSchemePlatformApiProperties properties) {
    return createClient(properties, FeeSchemePlatformRestClient.class);
  }

  /**
   * Creates a strongly-typed HTTP service client for the given API.
   *
   * <p>The API name is sourced from {@link ApiProperties#getName()} so that log labels and metric
   * tags are always consistent with the properties definition and never rely on hardcoded strings
   * at the call site.
   *
   * @param properties the API-specific configuration properties
   * @param clientType the {@link org.springframework.web.service.annotation.HttpExchange} interface
   *     to proxy
   * @param <T> the client interface type
   * @return a configured HTTP service client instance
   */
  private <T> T createClient(ApiProperties properties, Class<T> clientType) {
    return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(buildWebClient(properties)))
        .build()
        .createClient(clientType);
  }

  /**
   * Builds a {@link WebClient} fully configured for a single downstream API.
   *
   * <p>The following concerns are wired here:
   *
   * <ul>
   *   <li>Reactor Netty connection pool and TCP/response timeouts via {@link
   *       ReactorNettyHttpClientFactory}.
   *   <li>Structured request/response logging with credential-header redaction.
   *   <li>Micrometer timer metrics per API, method, URI, and status code.
   *   <li>Per-API in-memory codec buffer sized from {@link
   *       ApiProperties#getMaxInMemoryBufferBytes()}.
   * </ul>
   *
   * @param properties the API-specific configuration properties
   * @return a configured {@link WebClient} instance
   */
  private WebClient buildWebClient(ApiProperties properties) {
    String apiName = properties.getName();
    log.info(
        "Configuring WebClient [{}] - url: {}, connectionTimeoutMs: {}, responseTimeoutMs: {},"
            + " maxConnections: {}, pendingAcquireTimeoutMs: {}, maxInMemoryBufferBytes: {}",
        apiName,
        properties.getUrl(),
        properties.getConnectionTimeoutMs(),
        properties.getResponseTimeoutMs(),
        properties.getMaxConnections(),
        properties.getPendingAcquireTimeoutMs(),
        properties.getMaxInMemoryBufferBytes());

    ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(
                configurer ->
                    configurer
                        .defaultCodecs()
                        .maxInMemorySize(properties.getMaxInMemoryBufferBytes()))
            .build();

    return WebClient.builder()
        .clientConnector(ReactorNettyHttpClientFactory.create(apiName, properties))
        .baseUrl(properties.getUrl())
        .defaultHeader(properties.getAuthHeader(), properties.getAccessToken())
        .exchangeStrategies(strategies)
        .filter(new WebClientLoggingFilter(apiName))
        .filter(new WebClientMetricsFilter(meterRegistry, apiName))
        .build();
  }
}
