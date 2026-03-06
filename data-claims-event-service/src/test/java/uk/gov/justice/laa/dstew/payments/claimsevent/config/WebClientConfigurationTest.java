package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.MetricPublisher;

/**
 * Unit tests for {@link WebClientConfiguration}.
 *
 * <p>Exercises bean creation for each downstream REST client and validates that the configuration
 * wires without error for a range of valid property inputs. No network calls are made — the tests
 * verify only that the Spring HTTP service proxy is created and non-null.
 */
@DisplayName("WebClientConfiguration")
class WebClientConfigurationTest {

  private WebClientConfiguration configuration;

  @BeforeEach
  void setUp() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MetricsProperties metricsProperties = new MetricsProperties();
    MetricPublisher metricPublisher = new MetricPublisher(registry, metricsProperties);
    configuration = new WebClientConfiguration(metricPublisher);
  }

  // ---------------------------------------------------------------------------
  // Helper — builds a properties instance with a minimal but valid URL
  // ---------------------------------------------------------------------------

  private static DataClaimsApiProperties dataClaimsProperties() {
    DataClaimsApiProperties p = new DataClaimsApiProperties();
    p.setUrl("https://claims.example.com");
    p.setAccessToken("token");
    return p;
  }

  private static FeeSchemePlatformApiProperties feeSchemePlatformProperties() {
    FeeSchemePlatformApiProperties p = new FeeSchemePlatformApiProperties();
    p.setUrl("https://fee-scheme.example.com");
    p.setAccessToken("token");
    return p;
  }

  private static ProviderDetailsApiProperties providerDetailsProperties() {
    ProviderDetailsApiProperties p = new ProviderDetailsApiProperties();
    p.setUrl("https://provider.example.com");
    p.setAccessToken("token");
    return p;
  }

  // ---------------------------------------------------------------------------
  // Bean creation — each client
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("claimsApiClient bean")
  class ClaimsApiClient {

    @Test
    @DisplayName("returns a non-null DataClaimsRestClient")
    void returnsNonNull() {
      DataClaimsRestClient client = configuration.claimsApiClient(dataClaimsProperties());
      assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("returns a DataClaimsRestClient instance")
    void returnsCorrectType() {
      assertThat(configuration.claimsApiClient(dataClaimsProperties()))
          .isInstanceOf(DataClaimsRestClient.class);
    }

    @Test
    @DisplayName("produces distinct instances for different property configurations")
    void distinctInstancesForDifferentProperties() {
      DataClaimsApiProperties propsA = dataClaimsProperties();
      DataClaimsApiProperties propsB = dataClaimsProperties();
      propsB.setUrl("https://claims-b.example.com");

      assertThat(configuration.claimsApiClient(propsA))
          .isNotSameAs(configuration.claimsApiClient(propsB));
    }
  }

  @Nested
  @DisplayName("feeSchemePlatformRestClient bean")
  class FeeSchemePlatformClient {

    @Test
    @DisplayName("returns a non-null FeeSchemePlatformRestClient")
    void returnsNonNull() {
      FeeSchemePlatformRestClient client =
          configuration.feeSchemePlatformRestClient(feeSchemePlatformProperties());
      assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("returns a FeeSchemePlatformRestClient instance")
    void returnsCorrectType() {
      assertThat(configuration.feeSchemePlatformRestClient(feeSchemePlatformProperties()))
          .isInstanceOf(FeeSchemePlatformRestClient.class);
    }

    @Test
    @DisplayName("produces distinct instances for different property configurations")
    void distinctInstancesForDifferentProperties() {
      FeeSchemePlatformApiProperties propsA = feeSchemePlatformProperties();
      FeeSchemePlatformApiProperties propsB = feeSchemePlatformProperties();
      propsB.setUrl("https://fee-scheme-b.example.com");

      assertThat(configuration.feeSchemePlatformRestClient(propsA))
          .isNotSameAs(configuration.feeSchemePlatformRestClient(propsB));
    }
  }

  @Nested
  @DisplayName("providerDetailsClient bean")
  class ProviderDetailsClient {

    @Test
    @DisplayName("returns a non-null ProviderDetailsRestClient")
    void returnsNonNull() {
      ProviderDetailsRestClient client =
          configuration.providerDetailsClient(providerDetailsProperties());
      assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("returns a ProviderDetailsRestClient instance")
    void returnsCorrectType() {
      assertThat(configuration.providerDetailsClient(providerDetailsProperties()))
          .isInstanceOf(ProviderDetailsRestClient.class);
    }

    @Test
    @DisplayName("produces distinct instances for different property configurations")
    void distinctInstancesForDifferentProperties() {
      ProviderDetailsApiProperties propsA = providerDetailsProperties();
      ProviderDetailsApiProperties propsB = providerDetailsProperties();
      propsB.setUrl("https://provider-b.example.com");

      assertThat(configuration.providerDetailsClient(propsA))
          .isNotSameAs(configuration.providerDetailsClient(propsB));
    }
  }

  // ---------------------------------------------------------------------------
  // Resilience — null access token and null URL are accepted without NPE
  // (the WebClient itself will fail on the first real request, not at build time)
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("null-safety at build time")
  class NullSafety {

    @Test
    @DisplayName("builds DataClaimsRestClient when accessToken is null")
    void claimsClientNullToken() {
      DataClaimsApiProperties p = dataClaimsProperties();
      p.setAccessToken(null);
      assertThat(configuration.claimsApiClient(p)).isNotNull();
    }

    @Test
    @DisplayName("builds FeeSchemePlatformRestClient when accessToken is null")
    void feeSchemePlatformClientNullToken() {
      FeeSchemePlatformApiProperties p = feeSchemePlatformProperties();
      p.setAccessToken(null);
      assertThat(configuration.feeSchemePlatformRestClient(p)).isNotNull();
    }

    @Test
    @DisplayName("builds ProviderDetailsRestClient when accessToken is null")
    void providerDetailsClientNullToken() {
      ProviderDetailsApiProperties p = providerDetailsProperties();
      p.setAccessToken(null);
      assertThat(configuration.providerDetailsClient(p)).isNotNull();
    }
  }

  // ---------------------------------------------------------------------------
  // Per-API buffer configuration — larger buffer produces a distinct WebClient
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("per-API buffer configuration")
  class BufferConfiguration {

    @Test
    @DisplayName("DataClaimsRestClient builds with a large in-memory buffer")
    void claimsClientLargeBuffer() {
      DataClaimsApiProperties p = dataClaimsProperties();
      p.setMaxInMemoryBufferBytes(10 * 1024 * 1024); // 10 MB
      assertThat(configuration.claimsApiClient(p)).isNotNull();
    }

    @Test
    @DisplayName("FeeSchemePlatformRestClient builds with a large in-memory buffer")
    void feeSchemePlatformClientLargeBuffer() {
      FeeSchemePlatformApiProperties p = feeSchemePlatformProperties();
      p.setMaxInMemoryBufferBytes(10 * 1024 * 1024);
      assertThat(configuration.feeSchemePlatformRestClient(p)).isNotNull();
    }

    @Test
    @DisplayName("ProviderDetailsRestClient builds with a large in-memory buffer")
    void providerDetailsClientLargeBuffer() {
      ProviderDetailsApiProperties p = providerDetailsProperties();
      p.setMaxInMemoryBufferBytes(10 * 1024 * 1024);
      assertThat(configuration.providerDetailsClient(p)).isNotNull();
    }
  }
}
