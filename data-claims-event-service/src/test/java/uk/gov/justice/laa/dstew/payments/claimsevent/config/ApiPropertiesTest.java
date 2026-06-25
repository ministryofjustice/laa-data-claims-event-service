package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/**
 * Unit tests for {@link ApiProperties} and its concrete subclasses.
 *
 * <p>Verifies default values, setter/getter round-trips, and the per-subclass constructor defaults
 * (auth header, name, and {@code PREFIX} constant).
 */
@DisplayName("ApiProperties")
class ApiPropertiesTest {

  // -------------------------------------------------------------------------
  // ApiProperties — defaults
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("defaults")
  class Defaults {

    private final ApiProperties props = new ApiProperties();

    @Test
    @DisplayName("url is null by default")
    void urlIsNullByDefault() {
      assertThat(props.getUrl()).isNull();
    }

    @Test
    @DisplayName("name is null by default")
    void nameIsNullByDefault() {
      assertThat(props.getName()).isNull();
    }

    @Test
    @DisplayName("accessToken is null by default")
    void accessTokenIsNullByDefault() {
      assertThat(props.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("authHeader defaults to Authorization")
    void authHeaderDefaultsToAuthorization() {
      assertThat(props.getAuthHeader()).isEqualTo(HttpHeaders.AUTHORIZATION);
    }

    @Test
    @DisplayName("connectionTimeoutMs defaults to 2000")
    void connectionTimeoutMsDefault() {
      assertThat(props.getConnectionTimeoutMs()).isEqualTo(2_000);
    }

    @Test
    @DisplayName("responseTimeoutMs defaults to 10000")
    void responseTimeoutMsDefault() {
      assertThat(props.getResponseTimeoutMs()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("maxConnections defaults to 50")
    void maxConnectionsDefault() {
      assertThat(props.getMaxConnections()).isEqualTo(50);
    }

    @Test
    @DisplayName("pendingAcquireTimeoutMs defaults to 5000")
    void pendingAcquireTimeoutMsDefault() {
      assertThat(props.getPendingAcquireTimeoutMs()).isEqualTo(5_000);
    }

    @Test
    @DisplayName("maxInMemoryBufferBytes defaults to 256 KB")
    void maxInMemoryBufferBytesDefault() {
      assertThat(props.getMaxInMemoryBufferBytes()).isEqualTo(256 * 1024);
    }
  }

  // -------------------------------------------------------------------------
  // ApiProperties — setter / getter round-trips
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("setters and getters")
  class SettersAndGetters {

    private final ApiProperties props = new ApiProperties();

    @Test
    @DisplayName("setUrl / getUrl round-trips correctly")
    void urlRoundTrip() {
      props.setUrl("https://api.example.com");
      assertThat(props.getUrl()).isEqualTo("https://api.example.com");
    }

    @Test
    @DisplayName("setName / getName round-trips correctly")
    void nameRoundTrip() {
      props.setName("my-api");
      assertThat(props.getName()).isEqualTo("my-api");
    }

    @Test
    @DisplayName("setAccessToken / getAccessToken round-trips correctly")
    void accessTokenRoundTrip() {
      props.setAccessToken("token-abc");
      assertThat(props.getAccessToken()).isEqualTo("token-abc");
    }

    @Test
    @DisplayName("setAuthHeader / getAuthHeader round-trips correctly")
    void authHeaderRoundTrip() {
      props.setAuthHeader("X-Authorization");
      assertThat(props.getAuthHeader()).isEqualTo("X-Authorization");
    }

    @Test
    @DisplayName("setConnectionTimeoutMs / getConnectionTimeoutMs round-trips correctly")
    void connectionTimeoutMsRoundTrip() {
      props.setConnectionTimeoutMs(3_000);
      assertThat(props.getConnectionTimeoutMs()).isEqualTo(3_000);
    }

    @Test
    @DisplayName("setResponseTimeoutMs / getResponseTimeoutMs round-trips correctly")
    void responseTimeoutMsRoundTrip() {
      props.setResponseTimeoutMs(15_000);
      assertThat(props.getResponseTimeoutMs()).isEqualTo(15_000);
    }

    @Test
    @DisplayName("setMaxConnections / getMaxConnections round-trips correctly")
    void maxConnectionsRoundTrip() {
      props.setMaxConnections(100);
      assertThat(props.getMaxConnections()).isEqualTo(100);
    }

    @Test
    @DisplayName("setPendingAcquireTimeoutMs / getPendingAcquireTimeoutMs round-trips correctly")
    void pendingAcquireTimeoutMsRoundTrip() {
      props.setPendingAcquireTimeoutMs(8_000);
      assertThat(props.getPendingAcquireTimeoutMs()).isEqualTo(8_000);
    }

    @Test
    @DisplayName("setMaxInMemoryBufferBytes / getMaxInMemoryBufferBytes round-trips correctly")
    void maxInMemoryBufferBytesRoundTrip() {
      props.setMaxInMemoryBufferBytes(512 * 1024);
      assertThat(props.getMaxInMemoryBufferBytes()).isEqualTo(512 * 1024);
    }
  }

  // -------------------------------------------------------------------------
  // DataClaimsApiProperties
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("DataClaimsApiProperties")
  class DataClaimsApiPropertiesTests {

    private final DataClaimsApiProperties props = new DataClaimsApiProperties();

    @Test
    @DisplayName("PREFIX constant equals laa.claims-api")
    void prefixConstant() {
      assertThat(DataClaimsApiProperties.PREFIX).isEqualTo("laa.claims-api");
    }

    @Test
    @DisplayName("name is initialised to PREFIX by constructor")
    void nameInitialisedFromPrefix() {
      assertThat(props.getName()).isEqualTo(DataClaimsApiProperties.PREFIX);
    }

    @Test
    @DisplayName("authHeader defaults to Authorization")
    void authHeaderIsAuthorization() {
      assertThat(props.getAuthHeader()).isEqualTo(HttpHeaders.AUTHORIZATION);
    }

    @Test
    @DisplayName("inherits all ApiProperties defaults")
    void inheritsDefaults() {
      assertThat(props.getConnectionTimeoutMs()).isEqualTo(2_000);
      assertThat(props.getResponseTimeoutMs()).isEqualTo(10_000);
      assertThat(props.getMaxConnections()).isEqualTo(50);
      assertThat(props.getPendingAcquireTimeoutMs()).isEqualTo(5_000);
      assertThat(props.getMaxInMemoryBufferBytes()).isEqualTo(256 * 1024);
    }
  }

  // -------------------------------------------------------------------------
  // FeeSchemePlatformApiProperties
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("FeeSchemePlatformApiProperties")
  class FeeSchemePlatformApiPropertiesTests {

    private final FeeSchemePlatformApiProperties props = new FeeSchemePlatformApiProperties();

    @Test
    @DisplayName("PREFIX constant equals laa.fee-scheme-platform-api")
    void prefixConstant() {
      assertThat(FeeSchemePlatformApiProperties.PREFIX).isEqualTo("laa.fee-scheme-platform-api");
    }

    @Test
    @DisplayName("name is initialised to PREFIX by constructor")
    void nameInitialisedFromPrefix() {
      assertThat(props.getName()).isEqualTo(FeeSchemePlatformApiProperties.PREFIX);
    }

    @Test
    @DisplayName("authHeader defaults to Authorization")
    void authHeaderIsAuthorization() {
      assertThat(props.getAuthHeader()).isEqualTo(HttpHeaders.AUTHORIZATION);
    }

    @Test
    @DisplayName("inherits all ApiProperties defaults")
    void inheritsDefaults() {
      assertThat(props.getConnectionTimeoutMs()).isEqualTo(2_000);
      assertThat(props.getResponseTimeoutMs()).isEqualTo(10_000);
      assertThat(props.getMaxConnections()).isEqualTo(50);
      assertThat(props.getPendingAcquireTimeoutMs()).isEqualTo(5_000);
      assertThat(props.getMaxInMemoryBufferBytes()).isEqualTo(256 * 1024);
    }
  }

  // -------------------------------------------------------------------------
  // ProviderDetailsApiProperties
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("ProviderDetailsApiProperties")
  class ProviderDetailsApiPropertiesTests {

    private final ProviderDetailsApiProperties props = new ProviderDetailsApiProperties();

    @Test
    @DisplayName("PREFIX constant equals laa.provider-details-api")
    void prefixConstant() {
      assertThat(ProviderDetailsApiProperties.PREFIX).isEqualTo("laa.provider-details-api");
    }

    @Test
    @DisplayName("name is initialised to PREFIX by constructor")
    void nameInitialisedFromPrefix() {
      assertThat(props.getName()).isEqualTo(ProviderDetailsApiProperties.PREFIX);
    }

    @Test
    @DisplayName("authHeader is X-Authorization")
    void authHeaderIsXAuthorization() {
      assertThat(props.getAuthHeader()).isEqualTo("X-Authorization");
    }

    @Test
    @DisplayName("authHeader differs from the Authorization header used by other clients")
    void authHeaderDiffersFromStandardAuthorizationHeader() {
      assertThat(props.getAuthHeader()).isNotEqualTo(HttpHeaders.AUTHORIZATION);
    }

    @Test
    @DisplayName("inherits all ApiProperties defaults")
    void inheritsDefaults() {
      assertThat(props.getConnectionTimeoutMs()).isEqualTo(2_000);
      assertThat(props.getResponseTimeoutMs()).isEqualTo(10_000);
      assertThat(props.getMaxConnections()).isEqualTo(50);
      assertThat(props.getPendingAcquireTimeoutMs()).isEqualTo(5_000);
      assertThat(props.getMaxInMemoryBufferBytes()).isEqualTo(256 * 1024);
    }
  }
}
