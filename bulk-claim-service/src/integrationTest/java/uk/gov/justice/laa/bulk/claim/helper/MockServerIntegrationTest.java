package uk.gov.justice.laa.bulk.claim.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.text.SimpleDateFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.client.MockServerClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import uk.gov.justice.laa.bulk.claim.config.ApiProperties;
import uk.gov.justice.laa.bulk.claim.config.WebClientConfiguration;

public class MockServerIntegrationTest {

  protected static final DockerImageName MOCKSERVER_IMAGE =
      DockerImageName.parse("mockserver/mockserver")
          .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

  protected static MockServerContainer mockServerContainer;
  protected static MockServerClient mockServerClient;

  protected static ObjectMapper objectMapper = new ObjectMapper();

  @BeforeAll
  static void beforeEveryTest() {
    // Skip tests if Docker is unavailable
    Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Docker is not available, skipping the tests.");

    // Start MockServer container
    mockServerContainer =
        new MockServerContainer(MOCKSERVER_IMAGE)
            .waitingFor(
                Wait.forListeningPort()
                    .withStartupTimeout(
                        java.time.Duration.ofSeconds(
                            30))); // Increase startup timeout for reliability
    mockServerContainer.start();

    // Initialize MockServerClient
    mockServerClient =
        new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());

    // Setup object mapper
    objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            // Set to this format as that is the format provided by OpenAPI spec so will make
            // comparison easier
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  protected <T> T createClient(Class<T> serviceClass) {
    ApiProperties apiProperties =
        new ApiProperties(
            mockServerContainer.getEndpoint(),
            String.valueOf(mockServerContainer.getServerPort()),
            0,
            "");
    WebClient webClient = WebClientConfiguration.createWebClient(apiProperties);
    HttpServiceProxyFactory factory =
        HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
    return factory.createClient(serviceClass);
  }

  @AfterEach
  void tearDown() {
    mockServerClient.reset();
  }

  @AfterAll
  static void afterAll() {
    mockServerContainer.stop();
  }
}
