package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * {@link ExchangeFilterFunction} that records Micrometer {@link Timer} metrics for every outbound
 * HTTP request made by a WebClient.
 *
 * <p>A timer named {@code http.client.requests} is recorded with the following tags:
 *
 * <ul>
 *   <li>{@code api} — the name of the downstream API (e.g. {@code data-claims-api})
 *   <li>{@code method} — the HTTP method (e.g. {@code GET}, {@code POST})
 *   <li>{@code uri} — the request path (e.g. {@code /api/v1/submissions/abc})
 *   <li>{@code status} — the HTTP status code returned (e.g. {@code 200}), or {@code CLIENT_ERROR}
 *       if the exchange itself fails before a response is received
 * </ul>
 *
 * <p>These metrics are published to the Micrometer {@link MeterRegistry} and exposed via the {@code
 * /actuator/prometheus} scrape endpoint for collection by Prometheus.
 */
public class WebClientMetricsFilter implements ExchangeFilterFunction {

  private static final String METRIC_NAME = "http.client.requests";
  private static final String STATUS_CLIENT_ERROR = "CLIENT_ERROR";

  private final MeterRegistry meterRegistry;
  private final String apiName;

  /**
   * Constructs a metrics filter for the named downstream API.
   *
   * @param meterRegistry the Micrometer registry to which timers are published
   * @param apiName human-readable label used as the {@code api} tag value (e.g. {@code
   *     "data-claims-api"})
   */
  public WebClientMetricsFilter(MeterRegistry meterRegistry, String apiName) {
    this.meterRegistry = meterRegistry;
    this.apiName = apiName;
  }

  @Override
  @NonNull
  public Mono<ClientResponse> filter(
      @NonNull ClientRequest request, @NonNull ExchangeFunction next) {
    Instant start = Instant.now();
    String method = request.method().name();
    String uri = request.url().getPath();

    return next.exchange(request)
        .doOnNext(
            response ->
                recordTimer(method, uri, String.valueOf(response.statusCode().value()), start))
        .doOnError(error -> recordTimer(method, uri, STATUS_CLIENT_ERROR, start));
  }

  /**
   * Records a {@link Timer} sample for the completed exchange.
   *
   * @param method the HTTP method of the request
   * @param uri the request path
   * @param status the HTTP status code as a string, or {@code CLIENT_ERROR} if no response was
   *     received
   * @param start the {@link Instant} at which the request was initiated
   */
  private void recordTimer(String method, String uri, String status, Instant start) {
    Timer.builder(METRIC_NAME)
        .tag("api", apiName)
        .tag("method", method)
        .tag("uri", uri)
        .tag("status", status)
        .register(meterRegistry)
        .record(Duration.between(start, Instant.now()));
  }
}
