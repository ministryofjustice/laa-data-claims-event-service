package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import software.amazon.awssdk.regions.Region;

public class SqsConfigTest {
  private static final String ACCESS_KEY = "accessKey";
  private static final String SECRET_KEY = "secretKey";
  private static final String ENDPOINT = "http://localhost:8080";

  @DisplayName("Profile-test-wiremock-Should have access key and secret key in test environment")
  @Test
  public void testEnvironmentSqsConfig() {
    StandardEnvironment environment = new StandardEnvironment();
    environment.setActiveProfiles("test", "wiremock");
    SqsConfig sqsConfig = new SqsConfig();
    try (var actualResult =
        sqsConfig.sqsClientLocal(Region.US_EAST_1.toString(), ACCESS_KEY, SECRET_KEY, ENDPOINT)) {
      var endpoint =
          actualResult
              .serviceClientConfiguration()
              .endpointOverride()
              .map(URI::toString)
              .orElse("");
      assertThat(endpoint).isEqualTo(ENDPOINT);
      var region = actualResult.serviceClientConfiguration().region().toString();
      assertThat(region).isEqualTo(Region.US_EAST_1.toString());
    }
  }

  @DisplayName("Profile-default-Should not have access key and secret key in default environment")
  @Test
  public void defaultEnvironmentSqsConfig() {
    SqsConfig sqsConfig = new SqsConfig();
    try (var actualResult = sqsConfig.sqsClient(Region.US_EAST_1.toString())) {
      var endpoint =
          actualResult
              .serviceClientConfiguration()
              .endpointOverride()
              .map(URI::toString)
              .orElse("");
      assertThat(endpoint).isEqualTo("");
      var region = actualResult.serviceClientConfiguration().region().toString();
      assertThat(region).isEqualTo(Region.US_EAST_1.toString());
    }
  }
}
