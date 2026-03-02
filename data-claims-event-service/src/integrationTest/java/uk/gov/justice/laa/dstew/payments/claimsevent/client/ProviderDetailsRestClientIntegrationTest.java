package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

@ActiveProfiles("test")
@ImportTestcontainers(MessageListenerBase.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(MockServerIntegrationTest.ClaimsConfiguration.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.cloud.aws.sqs.enabled=false", // Disable AWS SQS functionality
      "laa.bulk-claim-queue.name=not-used", // Dummy queue name to avoid initialization issues
    })
class ProviderDetailsRestClientIntegrationTest extends MockServerIntegrationTest {

  public static final String PROVIDER_OFFICES = "/api/v1/provider-offices/";
  public static final String SCHEDULES_ENDPOINT = "/schedules";
  private ProviderDetailsRestClient providerDetailsRestClient;

  @BeforeEach
  void setUp() {
    // Configure WebClient and integrate it with ProviderDetailsRestService
    providerDetailsRestClient = createClient(ProviderDetailsRestClient.class);
  }

  @Nested
  @DisplayName("Get office schedules with effective date")
  class GetOfficeSchedulesWithEffectiveDate {

    @Test
    @DisplayName("Should return 200 response")
    void shouldReturn200Response() throws Exception {
      // Given
      String officeCode = "1234";
      String areaOfLaw = "CRIMINAL";
      LocalDate effectiveDate = LocalDate.of(2021, 1, 1);

      String expectedBody =
          readJsonFromFile("provider-details/get-firm-schedules-openapi-200.json");

      mockServerClient
          .when(
              HttpRequest.request()
                  .withMethod("GET")
                  .withPath(PROVIDER_OFFICES + officeCode + SCHEDULES_ENDPOINT)
                  .withQueryStringParameter("areaOfLaw", areaOfLaw))
          .respond(
              HttpResponse.response()
                  .withStatusCode(200)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      Mono<ProviderFirmOfficeContractAndScheduleDto> result =
          providerDetailsRestClient.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate);

      // Then
      Optional<ProviderFirmOfficeContractAndScheduleDto> providerFirmSummary =
          result.blockOptional();
      assertThat(providerFirmSummary).isPresent();
      // Check all fields mapped correctly by serializing the result and comparing to expected JSON
      String resultJson = objectMapper.writeValueAsString(providerFirmSummary.get());
      assertThatJsonMatches(expectedBody, resultJson);
    }

    @Test
    @DisplayName("Should return 200 response with requireOpenStatus parameter")
    void shouldReturn200ResponseWithRequireOpenStatusParameter() throws Exception {
      // Given
      String officeCode = "1234";
      String areaOfLaw = "CRIMINAL";
      LocalDate effectiveDate = LocalDate.of(2021, 1, 1);

      String expectedBody =
          readJsonFromFile("provider-details/get-firm-schedules-openapi-200.json");

      mockServerClient
          .when(
              HttpRequest.request()
                  .withMethod("GET")
                  .withPath(PROVIDER_OFFICES + officeCode + SCHEDULES_ENDPOINT)
                  .withQueryStringParameter("areaOfLaw", areaOfLaw)
                  .withQueryStringParameter("requireOpenStatus", "false"))
          .respond(
              HttpResponse.response()
                  .withStatusCode(200)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      Mono<ProviderFirmOfficeContractAndScheduleDto> result =
          providerDetailsRestClient.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate);

      // Then
      Optional<ProviderFirmOfficeContractAndScheduleDto> providerFirmSummary =
          result.blockOptional();
      assertThat(providerFirmSummary).isPresent();
      // Check all fields mapped correctly by serializing the result and comparing to expected JSON
      String resultJson = objectMapper.writeValueAsString(providerFirmSummary.get());
      assertThatJsonMatches(expectedBody, resultJson);
    }

    @Test
    @DisplayName("Should return 200 response with requireOpenStatus parameter set to true")
    void shouldReturn200ResponseWithRequireOpenStatusParameterSetToTrue() throws Exception {
      // Given
      String officeCode = "1234";
      String areaOfLaw = "CRIMINAL";
      LocalDate effectiveDate = LocalDate.of(2021, 1, 1);

      String expectedBody =
          readJsonFromFile("provider-details/get-firm-schedules-openapi-200.json");

      mockServerClient
          .when(
              HttpRequest.request()
                  .withMethod("GET")
                  .withPath(PROVIDER_OFFICES + officeCode + SCHEDULES_ENDPOINT)
                  .withQueryStringParameter("areaOfLaw", areaOfLaw)
                  .withQueryStringParameter("requireOpenStatus", "true"))
          .respond(
              HttpResponse.response()
                  .withStatusCode(200)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      Mono<ProviderFirmOfficeContractAndScheduleDto> result =
          providerDetailsRestClient.getProviderFirmSchedules(
              officeCode, areaOfLaw, effectiveDate, true);

      // Then
      Optional<ProviderFirmOfficeContractAndScheduleDto> providerFirmSummary =
          result.blockOptional();
      assertThat(providerFirmSummary).isPresent();
      // Check all fields mapped correctly by serializing the result and comparing to expected JSON
      String resultJson = objectMapper.writeValueAsString(providerFirmSummary.get());
      assertThatJsonMatches(expectedBody, resultJson);
    }

    @Test
    @DisplayName("Should return 200 response without areaOfLaw query parameter")
    void shouldReturn200ResponseWithoutAreaOfLaw() throws Exception {
      // Given
      String officeCode = "1234";
      String areaOfLaw = "";
      LocalDate effectiveDate = LocalDate.of(2021, 1, 1);

      String expectedBody =
          readJsonFromFile("provider-details/get-firm-schedules-openapi-200.json");

      mockServerClient
          .when(
              HttpRequest.request()
                  .withMethod("GET")
                  .withPath(PROVIDER_OFFICES + officeCode + SCHEDULES_ENDPOINT)
                  .withQueryStringParameter("areaOfLaw", areaOfLaw))
          .respond(
              HttpResponse.response()
                  .withStatusCode(200)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      Mono<ProviderFirmOfficeContractAndScheduleDto> result =
          providerDetailsRestClient.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate);

      // Then
      Optional<ProviderFirmOfficeContractAndScheduleDto> providerFirmSummary =
          result.blockOptional();
      assertThat(providerFirmSummary).isPresent();
      // Check all fields mapped correctly by serializing the result and comparing to expected JSON
      String resultJson = objectMapper.writeValueAsString(providerFirmSummary.get());
      assertThatJsonMatches(expectedBody, resultJson);
    }

    @Test
    @DisplayName("Should handle 204 response")
    void shouldHandle204Response() {
      // Given
      String officeCode = "1234";
      String areaOfLaw = "CRIMINAL";
      LocalDate effectiveDate = LocalDate.of(2021, 1, 1);

      mockServerClient
          .when(
              HttpRequest.request()
                  .withMethod("GET")
                  .withPath(PROVIDER_OFFICES + officeCode + SCHEDULES_ENDPOINT)
                  .withQueryStringParameter("areaOfLaw", areaOfLaw))
          .respond(
              HttpResponse.response()
                  .withStatusCode(204)
                  // Returns no body, so content type is set to text/html
                  .withHeader("Content-Type", "text/html"));

      // When
      Mono<ProviderFirmOfficeContractAndScheduleDto> result =
          providerDetailsRestClient
              .getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate)
              .onErrorResume(throwable -> Mono.empty());

      // Then
      Optional<ProviderFirmOfficeContractAndScheduleDto> providerFirmSummary =
          result.blockOptional();
      // Expect empty Optional as no body returned, but due to no active contracts therefor an
      assertThat(providerFirmSummary).isEmpty();
    }

    @Test
    @DisplayName("Should handle 500 response")
    void shouldHandle500Response() {
      // Given
      String officeCode = "1234";
      String areaOfLaw = "CRIMINAL";
      LocalDate effectiveDate = LocalDate.of(2021, 1, 1);

      mockServerClient
          .when(
              HttpRequest.request()
                  .withMethod("GET")
                  .withPath(PROVIDER_OFFICES + officeCode + SCHEDULES_ENDPOINT)
                  .withQueryStringParameter("areaOfLaw", areaOfLaw))
          .respond(
              HttpResponse.response()
                  .withStatusCode(500)
                  .withHeader("Content-Type", "application/json"));

      // When
      Mono<ProviderFirmOfficeContractAndScheduleDto> result =
          providerDetailsRestClient.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate);

      // Then
      assertThatThrownBy(result::block)
          .isInstanceOf(WebClientResponseException.class)
          .hasMessageContaining("500 Internal Server Error");
    }

    @Test
    @DisplayName("Should return 409 response")
    void shouldHandle409Response() {
      // Given
      String officeCode = "1234";
      String areaOfLaw = "CRIMINAL";
      LocalDate effectiveDate = LocalDate.of(2021, 1, 1);

      mockServerClient
          .when(
              HttpRequest.request()
                  .withMethod("GET")
                  .withPath(PROVIDER_OFFICES + officeCode + SCHEDULES_ENDPOINT)
                  .withQueryStringParameter("areaOfLaw", areaOfLaw))
          .respond(
              HttpResponse.response()
                  .withStatusCode(409)
                  .withHeader("Content-Type", "application/json"));

      // When
      Mono<ProviderFirmOfficeContractAndScheduleDto> result =
          providerDetailsRestClient.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate);

      // Then
      assertThatThrownBy(result::block)
          .isInstanceOf(WebClientResponseException.class)
          .hasMessageContaining(
              "409 Conflict from GET http://%s:%d/api/v1/provider-offices/1234/schedules"
                  .formatted(mockServerContainer.getHost(), mockServerContainer.getServerPort()));
    }
  }
}
