package uk.gov.justice.laa.dstew.payments.claimsevent.helper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockserver.model.JsonBody.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.http.HttpStatusCode;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ApiProperties;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.DataClaimsApiProperties;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.FeeSchemePlatformApiProperties;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ProviderDetailsApiProperties;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.WebClientConfiguration;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil;

@Slf4j
@TestInstance(Lifecycle.PER_CLASS)
public abstract class MockServerIntegrationTest {
  private static final String API_VERSION_0 = "/api/v0/";
  private static final String API_VERSION_1 = "/api/v1/";
  private static final String API_VERSION_2 = "/api/v2/";
  private static final String DATA_SUBMISSION_API_PATH = API_VERSION_0 + "submissions/";
  private static final String DATA_CLAIMS_API_PATH = API_VERSION_0 + "claims";
  private static final String PROVIDER_OFFICES = API_VERSION_1 + "provider-offices/";
  private static final String SCHEDULES_ENDPOINT = "/schedules";
  private static final String FEE_DETAILS = API_VERSION_1 + "fee-details/";
  private static final String FEE_CALCULATION = API_VERSION_1 + "fee-calculation";
  private static final String CLAIMS_ENDPOINT = "/claims/";

  protected static final DockerImageName MOCKSERVER_IMAGE =
      DockerImageName.parse("mockserver/mockserver")
          .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

  private static final MockServerContainer MOCK_SERVER_CONTAINER = createContainer();

  protected static MockServerContainer mockServerContainer;
  protected MockServerClient mockServerClient;
  protected static MockServerContainer mockStaticServerContainer = MOCK_SERVER_CONTAINER;

  protected ObjectMapper objectMapper = new ObjectMapper();

  private static MockServerContainer createContainer() {
    List<String> portBinding = Arrays.asList("30000:1080");
    MockServerContainer container =
        new MockServerContainer(MOCKSERVER_IMAGE)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));
    container.setPortBindings(portBinding);
    container.start();
    log.info("Started MockServer container on port: {}", container.getFirstMappedPort());
    return container;
  }

  @AfterEach
  void tearDown() {
    mockServerClient.reset();
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

  protected static <T> T createClient(Class<T> serviceClass) {
    WebClient webClient = createWebClient();
    HttpServiceProxyFactory factory =
        HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
    return factory.createClient(serviceClass);
  }

  protected static @NotNull WebClient createWebClient() {
    ApiProperties apiProperties =
        new ApiProperties(mockServerContainer.getEndpoint(), "", "Authorization");
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

  protected void stubForPostFeeCalculation(final String expectedResponse) throws Exception {
    mockServerClient
        .when(
            HttpRequest.request().withMethod(HttpMethod.POST.toString()).withPath(FEE_CALCULATION))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatusCode.OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(readJsonFromFile(expectedResponse)));
  }

  protected void stubForGteFeeDetails(final String feeCode, final String expectedResponse)
      throws Exception {
    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath(FEE_DETAILS + feeCode))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatusCode.OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(readJsonFromFile(expectedResponse)));
  }

  protected void stubForGetProviderOffice(
      final String officeCode, final List<Parameter> parameters, final String expectedResponse)
      throws Exception {
    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath(PROVIDER_OFFICES + officeCode + SCHEDULES_ENDPOINT)
                .withQueryStringParameters(parameters))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatusCode.OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(readJsonFromFile(expectedResponse)));
  }

  protected void stubForGetSubmission(final UUID submissionId, final String expectedResponse)
      throws Exception {
    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath(DATA_SUBMISSION_API_PATH + submissionId))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatusCode.OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(readJsonFromFile(expectedResponse)));
  }

  protected void stubForPathSubmissionWithClaimsId(final UUID submissionId, final String claimId) {
    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.PATCH.toString())
                .withPath(DATA_SUBMISSION_API_PATH + submissionId + CLAIMS_ENDPOINT + claimId))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatusCode.NO_CONTENT)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()));
  }

  protected void stubForGetClaims(final List<Parameter> parameters, final String expectedResponse)
      throws Exception {
    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath(DATA_CLAIMS_API_PATH)
                .withQueryStringParameters(parameters))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatusCode.OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(json(readJsonFromFile(expectedResponse))));
  }

  protected void stubForGetClaim(UUID submissionId, UUID claimId, String expectedResponse)
      throws Exception {
    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath(
                    API_VERSION_0 + "submissions/" + submissionId + CLAIMS_ENDPOINT + claimId))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatusCode.OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(json(readJsonFromFile(expectedResponse))));
  }

  protected void stubReturnNoClaims(UUID submissionId) throws Exception {
    String expectedBody = readJsonFromFile("data-claims/get-claims/no-claims.json");
    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod("GET")
                .withPath(API_VERSION_0 + "claims")
                .withQueryStringParameter("submissionId", submissionId.toString()))
        .respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(expectedBody));
  }

  protected void stubForUpdateSubmission(UUID submissionId) {
    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod("PATCH")
                .withPath(API_VERSION_0 + "submissions/" + submissionId.toString()))
        .respond(
            HttpResponse.response()
                .withStatusCode(204)
                .withHeader("Content-Type", "application/json"));
  }

  protected void stubForUpdateSubmissionWithBody(UUID submissionId, SubmissionPatch patch)
      throws JsonProcessingException {
    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod("PATCH")
                .withPath(API_VERSION_0 + "submissions/" + submissionId.toString())
                .withBody(json(objectMapper.writeValueAsString(patch))))
        .respond(
            HttpResponse.response()
                .withStatusCode(204)
                .withHeader("Content-Type", "application/json"));
  }

  protected void getStubForGetSubmissionByCriteria(
      final List<Parameter> parameters, final String expectedResponse) throws Exception {
    List<Parameter> allParameters =
        Stream.concat(
                parameters.stream(),
                Stream.of(Parameter.param("size", "0"), Parameter.param("page", "0")))
            .toList();

    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath(API_VERSION_0 + "submissions")
                .withQueryStringParameters(allParameters))
        //                .withQueryStringParameters(
        //                    Parameter.param("size", "0"), Parameter.param("page", "0")))
        .respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(json(readJsonFromFile(expectedResponse))));
  }

  @TestConfiguration
  public static class ClaimsConfiguration {

    @Bean
    @Primary
    DataClaimsApiProperties dataClaimsApiProperties() {
      // Set using host and port running the mock server
      return new DataClaimsApiProperties("http://localhost:30000", "");
    }

    @Bean
    @Primary
    FeeSchemePlatformApiProperties feeSchemePlatformApiProperties() {
      // Set using host and port running the mock server
      return new FeeSchemePlatformApiProperties("http://localhost:30000", "");
    }

    @Bean
    @Primary
    ProviderDetailsApiProperties providerDetailsApiProperties() {
      // Set using host and port running the mock server
      return new ProviderDetailsApiProperties("http://localhost:30000", "");
    }

    @Bean
    @Primary
    DateUtil dateUtil() {
      return new DateUtil() {
        @Override
        public YearMonth currentYearMonth() {
          // Set current year to 2025-05 for constant values within code
          return YearMonth.of(2025, 5);
        }
      };
    }
  }
}
