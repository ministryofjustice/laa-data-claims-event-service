package uk.gov.justice.laa.bulk.claim.service.provider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.bulk.claim.service.provider.dto.ProviderFirmOfficeContractAndSchedule;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProviderDetailsRestServiceIT {

  private static final DockerImageName MOCKSERVER_IMAGE =
      DockerImageName.parse("mockserver/mockserver")
          .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

  private MockServerContainer mockServerContainer;
  private MockServerClient mockServerClient;

  private ObjectMapper objectMapper = new ObjectMapper();

  private ProviderDetailsRestService providerDetailsRestService;

  @BeforeEach
  void setUp() {
    // Skip tests if Docker is unavailable
    Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Docker is not available, skipping the tests.");

    // Start MockServer container
    mockServerContainer =
        new MockServerContainer(MOCKSERVER_IMAGE).waitingFor(Wait.forListeningPort());
    mockServerContainer.start();

    // Initialize MockServerClient
    mockServerClient =
        new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());

    // Configure WebClient and integrate it with ProviderDetailsRestService
    WebClient webClient = WebClient.builder().baseUrl(mockServerContainer.getEndpoint()).build();
    HttpServiceProxyFactory factory =
        HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
    providerDetailsRestService = factory.createClient(ProviderDetailsRestService.class);

    objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            // Set to this format as that is the format provided by OpenAPI spec so will make
            // comparison easier
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @AfterEach
  void tearDown() {
    if (mockServerClient != null) {
      mockServerClient.reset();
      mockServerContainer.stop();
    }
  }

  @Test
  @DisplayName("Test 200 response")
  void test200Response() throws Exception {
    // Given
    String officeCode = "1234";
    String areaOfLaw = "CRIMINAL";

    String expectedBody = readJsonFromFile("provide-detail-firm-schedules-openapi-200.json");

    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/provider-offices/" + officeCode + "/schedules")
                .withQueryStringParameter("areaOfLaw", areaOfLaw))
        .respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(expectedBody));

    // When
    Mono<ProviderFirmOfficeContractAndSchedule> result =
        providerDetailsRestService.getProviderFirmSchedules(officeCode, areaOfLaw);

    // Then
    Optional<ProviderFirmOfficeContractAndSchedule> providerFirmSummary = result.blockOptional();
    assertThat(providerFirmSummary).isPresent();
    // Check all fields mapped correctly by serializing the result and comparing to expected JSON
    String resultJson = objectMapper.writeValueAsString(providerFirmSummary.get());
    assertThatJsonMatches(expectedBody, resultJson);
  }

  @Test
  @DisplayName("Test 204 response")
  void test204Response() {
    // Given
    String officeCode = "1234";
    String areaOfLaw = "CRIMINAL";

    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/provider-offices/" + officeCode + "/schedules")
                .withQueryStringParameter("areaOfLaw", areaOfLaw))
        .respond(
            HttpResponse.response()
                .withStatusCode(204)
                .withHeader("Content-Type", "application/json"));

    // When
    Mono<ProviderFirmOfficeContractAndSchedule> result =
        providerDetailsRestService.getProviderFirmSchedules(officeCode, areaOfLaw);

    // Then
    Optional<ProviderFirmOfficeContractAndSchedule> providerFirmSummary = result.blockOptional();
    // Expect empty Optional as no body returned, but due to no active contracts therefor an
    // exception
    //  should not be thrown.
    assertThat(providerFirmSummary).isEmpty();
  }

  @Test
  @DisplayName("Test 500 response")
  void test500Response() {
    // Given
    String officeCode = "1234";
    String areaOfLaw = "CRIMINAL";

    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/provider-offices/" + officeCode + "/schedules")
                .withQueryStringParameter("areaOfLaw", areaOfLaw))
        .respond(
            HttpResponse.response()
                .withStatusCode(500)
                .withHeader("Content-Type", "application/json"));

    // When
    Mono<ProviderFirmOfficeContractAndSchedule> result =
        providerDetailsRestService.getProviderFirmSchedules(officeCode, areaOfLaw);

    // Then
    assertThatThrownBy(result::block)
        .isInstanceOf(WebClientResponseException.class)
        .hasMessageContaining("500 Internal Server Error");
  }

  @Test
  @DisplayName("Test 409 response")
  void test409Response() {
    // Given
    String officeCode = "1234";
    String areaOfLaw = "CRIMINAL";

    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/provider-offices/" + officeCode + "/schedules")
                .withQueryStringParameter("areaOfLaw", areaOfLaw))
        .respond(
            HttpResponse.response()
                .withStatusCode(409)
                .withHeader("Content-Type", "application/json"));

    // When
    Mono<ProviderFirmOfficeContractAndSchedule> result =
        providerDetailsRestService.getProviderFirmSchedules(officeCode, areaOfLaw);

    // Then
    assertThatThrownBy(result::block)
        .isInstanceOf(WebClientResponseException.class)
        .hasMessageContaining(
            "409 Conflict from GET http://localhost:%d/provider-offices/1234/schedules"
                .formatted(mockServerContainer.getServerPort()));
  }

  private static String readJsonFromFile(final String fileName) throws Exception {
    Path path = Paths.get("src/test/resources/responses", fileName);
    return Files.readString(path);
  }

  private static void assertThatJsonMatches(final String expectedJson, final String actualJson) {
    // Remove whitespace to make comparison easier
    String normalizedExpected = expectedJson.replaceAll("\\s+", "");
    String normalizedActual = actualJson.replaceAll("\\s+", "");
    assertThat(normalizedActual).isEqualTo(normalizedExpected);
  }
}
