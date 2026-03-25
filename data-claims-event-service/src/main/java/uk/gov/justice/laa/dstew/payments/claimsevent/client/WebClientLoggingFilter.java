package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * {@link ExchangeFilterFunction} that provides failure-focused logging for outbound HTTP requests.
 *
 * <p>Successful responses (1xx / 2xx / 3xx) are intentionally silent to avoid log noise in normal
 * operation. Only the following conditions produce log output:
 *
 * <ul>
 *   <li><b>4xx</b> — logged at {@code WARN} with method, path, status, and elapsed time.
 *   <li><b>5xx</b> — logged at {@code ERROR} with method, path, status, and elapsed time.
 *   <li><b>No response received</b> (timeout, connection refused, DNS failure, etc.) — logged at
 *       {@code ERROR} with the exception type, message, and elapsed time.
 * </ul>
 */
@Slf4j
public class WebClientLoggingFilter implements ExchangeFilterFunction {

  private static final String RESPONSE_LOG_FORMAT = "[{}] {} {} -> {} in {} ms";
  private static final String ERROR_LOG_FORMAT = "[{}] {} {} failed after {} ms — {} : {}";

  private final String apiName;

  /**
   * Constructs a logging filter for the named API.
   *
   * @param apiName human-readable label used as the log prefix (e.g. {@code "data-claims-api"})
   */
  public WebClientLoggingFilter(String apiName) {
    this.apiName = apiName;
  }

  @Override
  @NonNull
  public Mono<ClientResponse> filter(
      @NonNull ClientRequest request, @NonNull ExchangeFunction next) {
    Instant start = Instant.now();
    String method = request.method().name();
    String path = request.url().getPath();

    return next.exchange(request)
        .doOnNext(response -> logResponseIfUnsuccessful(method, path, response, start))
        .doOnError(error -> logError(method, path, error, start));
  }

  private void logResponseIfUnsuccessful(
      String method, String path, ClientResponse response, Instant start) {
    var statusCode = response.statusCode();
    if (!statusCode.isError()) {
      return; // 1xx / 2xx / 3xx — intentionally silent
    }

    Object[] args = {apiName, method, path, statusCode.value(), elapsedMs(start)};

    if (statusCode.is5xxServerError()) {
      log.error(RESPONSE_LOG_FORMAT, args);
    } else {
      log.warn(RESPONSE_LOG_FORMAT, args);
    }
  }

  private void logError(String method, String path, Throwable error, Instant start) {
    log.error(
        ERROR_LOG_FORMAT,
        apiName,
        method,
        path,
        elapsedMs(start),
        error.getClass().getSimpleName(),
        error.getMessage());
  }

  private long elapsedMs(Instant start) {
    return Duration.between(start, Instant.now()).toMillis();
  }
}
