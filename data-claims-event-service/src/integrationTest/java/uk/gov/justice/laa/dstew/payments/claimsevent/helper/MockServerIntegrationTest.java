package uk.gov.justice.laa.dstew.payments.claimsevent.helper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockserver.client.MockServerClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ApiProperties;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.WebClientConfiguration;

@Slf4j
@TestInstance(Lifecycle.PER_CLASS)
public abstract class MockServerIntegrationTest {

  protected static final DockerImageName MOCKSERVER_IMAGE =
      DockerImageName.parse("mockserver/mockserver")
          .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

  private static final MockServerContainer MOCK_SERVER_CONTAINER = createContainer();

  protected MockServerContainer mockServerContainer;
  protected MockServerClient mockServerClient;

  protected ObjectMapper objectMapper = new ObjectMapper();

  private static MockServerContainer createContainer() {
    MockServerContainer container =
        new MockServerContainer(MOCKSERVER_IMAGE)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));
    container.start();
    log.info("Started MockServer container on port: {}", container.getFirstMappedPort());
    return container;
  }

  @BeforeAll
  void beforeEveryTest() {
    // Skip tests if Docker is unavailable
    Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Docker is not available, skipping the tests.");

    // Start MockServer container
    mockServerContainer = MOCK_SERVER_CONTAINER;

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
    WebClient webClient = createWebClient();
    HttpServiceProxyFactory factory =
        HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
    return factory.createClient(serviceClass);
  }

  protected @NotNull WebClient createWebClient() {
    ApiProperties apiProperties =
        new ApiProperties(
            mockServerContainer.getEndpoint(),
            String.valueOf(mockServerContainer.getServerPort()),
            0,
            "",
            "Authorization");
    return WebClientConfiguration.createWebClient(apiProperties);
  }

  protected static String readJsonFromFile(final String fileName) throws Exception {
    Path path = Paths.get("src/integrationTest/resources/responses", fileName);
    return Files.readString(path);
  }

  protected static void assertThatJsonMatches(final String expectedJson, final String actualJson) {
    // Remove whitespace to make comparison easier
    String normalizedExpected = expectedJson.replaceAll("\\s+", "");
    String normalizedActual = actualJson.replaceAll("\\s+", "");
    assertThat(normalizedActual).isEqualTo(normalizedExpected);
  }

  @AfterEach
  void tearDown() {
    mockServerClient.reset();
  }
}
