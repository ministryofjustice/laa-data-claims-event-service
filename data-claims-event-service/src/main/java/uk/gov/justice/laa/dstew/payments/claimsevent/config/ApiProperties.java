package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpHeaders;

/**
 * Base configuration properties shared by all downstream REST API clients.
 *
 * <p>Each subclass binds its own {@code @ConfigurationProperties} prefix so that timeout, pool, and
 * buffer settings can be tuned independently per API without affecting the others.
 *
 * <p>All fields are bound via setter injection by Spring's {@code @ConfigurationProperties}
 * mechanism. Defaults defined here serve as fallbacks when no value is present in the active
 * configuration.
 */
@Getter
@Setter
@NoArgsConstructor
public class ApiProperties {

  /** Base URL of the downstream API (e.g. {@code https://api.example.com}). */
  private String url;

  /**
   * Short identifying name for this API client, used as a label in log output and Prometheus
   * metrics (e.g. {@code data-claims-api}). Set by each subclass constructor and not expected to be
   * overridden via configuration.
   */
  private String name;

  /** Bearer token or equivalent credential used to authenticate outbound requests. */
  private String accessToken;

  /**
   * HTTP header name used to carry the access token (e.g. {@code Authorization} or {@code
   * X-Authorization}).
   */
  private String authHeader = HttpHeaders.AUTHORIZATION;

  /**
   * TCP connection-establishment timeout in milliseconds. The call fails immediately if a
   * connection cannot be opened within this time. Default: {@code 2000} ms.
   */
  private int connectionTimeoutMs = 2_000;

  /**
   * Maximum time in milliseconds allowed for a complete HTTP response to be received after the
   * request has been sent. Default: {@code 10000} ms.
   */
  private int responseTimeoutMs = 10_000;

  /**
   * Maximum number of connections held in the Reactor Netty connection pool for this API. Default:
   * {@code 50}.
   */
  private int maxConnections = 50;

  /**
   * Maximum time in milliseconds to wait for a connection to become available from the pool before
   * the request fails with an {@code AcquireTimeoutException}. Default: {@code 5000} ms.
   */
  private int pendingAcquireTimeoutMs = 5_000;

  /**
   * Maximum in-memory buffer size in bytes for deserialising response bodies. Raise this only for
   * APIs that can return large payloads. Default: {@code 262144} bytes (256 KB — the Spring WebFlux
   * default).
   */
  private int maxInMemoryBufferBytes = 256 * 1024;
}
