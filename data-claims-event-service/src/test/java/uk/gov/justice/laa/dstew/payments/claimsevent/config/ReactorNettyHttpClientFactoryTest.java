package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

/**
 * Unit tests for {@link ReactorNettyHttpClientFactory}.
 *
 * <p>Verifies that the factory produces a non-null {@link ReactorClientHttpConnector} and correctly
 * reflects the supplied {@link ApiProperties} values. Because Reactor Netty's {@link
 * reactor.netty.http.client.HttpClient} exposes no public getters for the configured timeouts and
 * pool parameters, the tests focus on construction success, non-null output, and observable
 * differences between configurations.
 */
@DisplayName("ReactorNettyHttpClientFactory")
class ReactorNettyHttpClientFactoryTest {

  private ApiProperties properties;

  @BeforeEach
  void setUp() {
    properties = new ApiProperties();
    properties.setUrl("https://api.example.com");
    properties.setName("test-api");
    properties.setConnectionTimeoutMs(1_000);
    properties.setResponseTimeoutMs(5_000);
    properties.setMaxConnections(10);
    properties.setPendingAcquireTimeoutMs(2_000);
  }

  @Test
  @DisplayName("returns a non-null ReactorClientHttpConnector")
  void returnsNonNullConnector() {
    ReactorClientHttpConnector connector =
        ReactorNettyHttpClientFactory.create("test-api", properties);
    assertThat(connector).isNotNull();
  }

  @Test
  @DisplayName("produces a distinct connector instance per invocation")
  void eachCallProducesDistinctInstance() {
    ReactorClientHttpConnector first = ReactorNettyHttpClientFactory.create("test-api", properties);
    ReactorClientHttpConnector second =
        ReactorNettyHttpClientFactory.create("test-api", properties);
    assertThat(first).isNotSameAs(second);
  }

  @Test
  @DisplayName("produces a distinct connector per api name")
  void distinctConnectorsForDifferentApiNames() {
    ReactorClientHttpConnector connectorA =
        ReactorNettyHttpClientFactory.create("api-a", properties);
    ReactorClientHttpConnector connectorB =
        ReactorNettyHttpClientFactory.create("api-b", properties);
    assertThat(connectorA).isNotSameAs(connectorB);
  }

  @Test
  @DisplayName("accepts minimum viable property values without throwing")
  void acceptsMinimalProperties() {
    ApiProperties minimal = new ApiProperties();
    minimal.setConnectionTimeoutMs(1);
    minimal.setResponseTimeoutMs(1);
    minimal.setMaxConnections(1);
    minimal.setPendingAcquireTimeoutMs(1);

    assertThat(ReactorNettyHttpClientFactory.create("minimal-api", minimal)).isNotNull();
  }

  @Test
  @DisplayName("accepts default ApiProperties values without throwing")
  void acceptsDefaultProperties() {
    assertThat(ReactorNettyHttpClientFactory.create("default-api", new ApiProperties()))
        .isNotNull();
  }

  @Test
  @DisplayName("accepts DataClaimsApiProperties without throwing")
  void acceptsDataClaimsApiProperties() {
    DataClaimsApiProperties props = new DataClaimsApiProperties();
    props.setUrl("https://claims.example.com");
    assertThat(ReactorNettyHttpClientFactory.create(props.getName(), props)).isNotNull();
  }

  @Test
  @DisplayName("accepts FeeSchemePlatformApiProperties without throwing")
  void acceptsFeeSchemePlatformApiProperties() {
    FeeSchemePlatformApiProperties props = new FeeSchemePlatformApiProperties();
    props.setUrl("https://fee-scheme.example.com");
    assertThat(ReactorNettyHttpClientFactory.create(props.getName(), props)).isNotNull();
  }

  @Test
  @DisplayName("accepts ProviderDetailsApiProperties without throwing")
  void acceptsProviderDetailsApiProperties() {
    ProviderDetailsApiProperties props = new ProviderDetailsApiProperties();
    props.setUrl("https://provider.example.com");
    assertThat(ReactorNettyHttpClientFactory.create(props.getName(), props)).isNotNull();
  }

  // ---------------------------------------------------------------------------
  // Edge cases — invalid and extreme property values
  // ---------------------------------------------------------------------------

  /**
   * Reactor Netty enforces that {@code maxConnections} is strictly positive and throws {@link
   * IllegalArgumentException} at pool construction time — these tests document and pin that
   * contract so a future library upgrade cannot silently remove the guard.
   *
   * <p>Timeout fields (connection, response, pendingAcquire) are passed through to the underlying
   * Netty channel options or {@link reactor.netty.http.client.HttpClient#responseTimeout} without
   * range validation at build time. Invalid values (zero, negative) will only produce errors at the
   * point a real request is made, which is consistent with Reactor Netty's design. Tests below
   * confirm this silent-acceptance behaviour so that any future change in library behaviour is
   * immediately visible.
   *
   * <p>{@code Integer.MAX_VALUE} for all numeric fields is accepted at build time by both Reactor
   * Netty and Netty's {@code ChannelOption.CONNECT_TIMEOUT_MILLIS}, confirming no overflow guard is
   * applied during construction.
   */
  @Nested
  @DisplayName("edge cases — invalid and extreme values")
  class EdgeCases {

    @Test
    @DisplayName("throws IllegalArgumentException when maxConnections is zero")
    void throwsWhenMaxConnectionsIsZero() {
      ApiProperties p = new ApiProperties();
      p.setMaxConnections(0);

      assertThatThrownBy(() -> ReactorNettyHttpClientFactory.create("edge-zero", p))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max Connections value must be strictly positive");
    }

    @Test
    @DisplayName("throws IllegalArgumentException when maxConnections is negative")
    void throwsWhenMaxConnectionsIsNegative() {
      ApiProperties p = new ApiProperties();
      p.setMaxConnections(-1);

      assertThatThrownBy(() -> ReactorNettyHttpClientFactory.create("edge-neg", p))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max Connections value must be strictly positive");
    }

    @Test
    @DisplayName("accepts negative connectionTimeoutMs at build time without throwing")
    void acceptsNegativeConnectionTimeout() {
      // Reactor Netty does not validate timeout values at construction time;
      // an invalid value would only surface when a real connection is attempted.
      ApiProperties p = new ApiProperties();
      p.setConnectionTimeoutMs(-1);

      assertThat(ReactorNettyHttpClientFactory.create("edge-neg-conn", p)).isNotNull();
    }

    @Test
    @DisplayName("accepts negative responseTimeoutMs at build time without throwing")
    void acceptsNegativeResponseTimeout() {
      ApiProperties p = new ApiProperties();
      p.setResponseTimeoutMs(-1);

      assertThat(ReactorNettyHttpClientFactory.create("edge-neg-resp", p)).isNotNull();
    }

    @Test
    @DisplayName("accepts negative pendingAcquireTimeoutMs at build time without throwing")
    void acceptsNegativePendingAcquireTimeout() {
      ApiProperties p = new ApiProperties();
      p.setPendingAcquireTimeoutMs(-1);

      assertThat(ReactorNettyHttpClientFactory.create("edge-neg-pending", p)).isNotNull();
    }

    @Test
    @DisplayName("accepts Integer.MAX_VALUE for maxConnections without throwing")
    void acceptsMaxIntMaxConnections() {
      ApiProperties p = new ApiProperties();
      p.setMaxConnections(Integer.MAX_VALUE);

      assertThat(ReactorNettyHttpClientFactory.create("edge-max-conn", p)).isNotNull();
    }

    @Test
    @DisplayName("accepts Integer.MAX_VALUE for all timeout fields without throwing")
    void acceptsMaxIntTimeouts() {
      ApiProperties p = new ApiProperties();
      p.setConnectionTimeoutMs(Integer.MAX_VALUE);
      p.setResponseTimeoutMs(Integer.MAX_VALUE);
      p.setPendingAcquireTimeoutMs(Integer.MAX_VALUE);

      assertThat(ReactorNettyHttpClientFactory.create("edge-max-timeouts", p)).isNotNull();
    }
  }
}
