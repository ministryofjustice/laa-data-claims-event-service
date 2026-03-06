package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Factory responsible for constructing a Reactor Netty {@link HttpClient} that is fully configured
 * from an {@link ApiProperties} instance.
 *
 * <p>Each downstream API gets its own {@link ConnectionProvider} (named after the API) so that
 * connection-pool metrics are labelled and observable independently in Prometheus/Grafana.
 *
 * <p>Settings controlled per API:
 *
 * <ul>
 *   <li><b>connectionTimeoutMs</b> — TCP connect timeout; the call fails immediately if a
 *       connection cannot be established within this window.
 *   <li><b>responseTimeoutMs</b> — total time allowed from request sent to full response received.
 *   <li><b>maxConnections</b> — ceiling on pooled connections; prevents unbounded resource use
 *       under high concurrency.
 *   <li><b>pendingAcquireTimeoutMs</b> — how long a caller waits for a pool slot before receiving
 *       an {@code AcquireTimeoutException}; guards against indefinite queuing.
 * </ul>
 */
public final class ReactorNettyHttpClientFactory {

  private ReactorNettyHttpClientFactory() {}

  /**
   * Builds a {@link ReactorClientHttpConnector} configured from the supplied {@link ApiProperties}.
   *
   * <p>A named {@link ConnectionProvider} is created for the given {@code apiName} so that Reactor
   * Netty pool metrics ({@code reactor.netty.connection.provider.*}) are labelled with the API
   * name, making them distinguishable in Prometheus.
   *
   * @param apiName human-readable name used to label the connection pool (e.g. {@code
   *     "data-claims-api"})
   * @param properties the API-specific configuration properties
   * @return a configured {@link ReactorClientHttpConnector} ready to be supplied to a {@link
   *     org.springframework.web.reactive.function.client.WebClient.Builder}
   */
  public static ReactorClientHttpConnector create(String apiName, ApiProperties properties) {
    ConnectionProvider connectionProvider =
        ConnectionProvider.builder(apiName)
            .maxConnections(properties.getMaxConnections())
            .pendingAcquireTimeout(Duration.ofMillis(properties.getPendingAcquireTimeoutMs()))
            .metrics(true)
            .build();

    HttpClient httpClient =
        HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectionTimeoutMs())
            .responseTimeout(Duration.ofMillis(properties.getResponseTimeoutMs()));

    return new ReactorClientHttpConnector(httpClient);
  }
}
