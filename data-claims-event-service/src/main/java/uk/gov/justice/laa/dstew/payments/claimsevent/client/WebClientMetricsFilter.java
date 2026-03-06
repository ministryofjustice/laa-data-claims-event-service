package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import org.jspecify.annotations.NonNull;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.MetricNames;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.MetricPublisher;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.MetricSample;

/**
 * {@link ExchangeFilterFunction} that records latency metrics for every outbound HTTP request made
 * by a WebClient.
 *
 * <p>A timer named {@code <namespace>http_client_requests} is recorded with the following tags:
 *
 * <ul>
 *   <li>{@code api} — the name of the downstream API (e.g. {@code data-claims-api})
 *   <li>{@code method} — the HTTP method (e.g. {@code GET}, {@code POST})
 *   <li>{@code uri} — the request path (e.g. {@code /api/v1/submissions/abc})
 *   <li>{@code status} — the HTTP status code (e.g. {@code 200}), or {@code CLIENT_ERROR} if the
 *       exchange fails before a response is received
 * </ul>
 *
 * <p>All metric configuration — namespace, registry, percentiles — is managed by the injected
 * {@link MetricPublisher}. This class has no direct dependency on any Micrometer type.
 */
public class WebClientMetricsFilter implements ExchangeFilterFunction {

  private static final String STATUS_CLIENT_ERROR = "CLIENT_ERROR";

  private final MetricPublisher metricPublisher;
  private final String apiName;

  /**
   * Constructs a metrics filter for the named downstream API.
   *
   * @param metricPublisher the publisher used to start and record timers
   * @param apiName human-readable label used as the {@code api} tag value (e.g. {@code
   *     "data-claims-api"})
   */
  public WebClientMetricsFilter(MetricPublisher metricPublisher, String apiName) {
    this.metricPublisher = metricPublisher;
    this.apiName = apiName;
  }

  @Override
  @NonNull
  public Mono<ClientResponse> filter(
      @NonNull ClientRequest request, @NonNull ExchangeFunction next) {
    String method = request.method().name();
    String uri = request.url().getPath();
    MetricSample sample =
        metricPublisher.startTimer(
            MetricNames.HTTP_CLIENT_REQUESTS,
            MetricNames.TAG_API,
            apiName,
            MetricNames.TAG_METHOD,
            method,
            MetricNames.TAG_URI,
            uri);

    return next.exchange(request)
        .doOnNext(
            response ->
                sample.stop(MetricNames.TAG_STATUS, String.valueOf(response.statusCode().value())))
        .doOnError(error -> sample.stop(MetricNames.TAG_STATUS, STATUS_CLIENT_ERROR));
  }
}
