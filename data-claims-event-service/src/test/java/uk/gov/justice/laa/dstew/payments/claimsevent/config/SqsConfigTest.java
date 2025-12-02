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

  @DisplayName("Should have access key and secret key in test environment")
  @Test
  public void testEnvironmentSqsConfig() {
    StandardEnvironment environment = new StandardEnvironment();
    environment.setActiveProfiles("test");
    SqsConfig sqsConfig = new SqsConfig();
    try (var actualResult =
        sqsConfig.sqsClient(
            Region.US_EAST_1.toString(), ACCESS_KEY, SECRET_KEY, ENDPOINT, environment)) {
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

  @DisplayName("Should not have access key and secret key in default environment")
  @Test
  public void defaultEnvironmentSqsConfig() {
    StandardEnvironment environment = new StandardEnvironment();
    environment.setActiveProfiles("default");
    SqsConfig sqsConfig = new SqsConfig();
    try (var actualResult =
        sqsConfig.sqsClient(
            Region.US_EAST_1.toString(), ACCESS_KEY, SECRET_KEY, ENDPOINT, environment)) {
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
