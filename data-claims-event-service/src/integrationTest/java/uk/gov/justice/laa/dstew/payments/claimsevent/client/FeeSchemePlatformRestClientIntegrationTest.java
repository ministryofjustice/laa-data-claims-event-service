package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationRequest;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.cloud.aws.sqs.enabled=false", // Disable AWS SQS functionality
      "laa.bulk-claim-queue.name=not-used", // Dummy queue name to avoid initialization issues
    })
class FeeSchemePlatformRestClientIntegrationTest extends MockServerIntegrationTest {

  private FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  @BeforeEach
  void setUp() {
    // Configure WebClient and integrate it with FeeSchemePlatformRestClient
    feeSchemePlatformRestClient = createClient(FeeSchemePlatformRestClient.class);
  }

  @Nested
  @DisplayName("GET: /fee-details/{feeCode} tests")
  class GetFeeDetailsTests {

    @Test
    @DisplayName("Should handle 200 response")
    void shouldHandle200Responses() throws Exception {
      // Given
      String feeCode = "AB12";

      String expectedBody = readJsonFromFile("fee-scheme/get-fee-details-200.json");

      mockServerClient
          .when(
              HttpRequest.request()
                  .withMethod("GET")
                  .withPath("/api/v0/fee-details/" + feeCode))
          .respond(
              HttpResponse.response()
                  .withStatusCode(200)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      ResponseEntity<FeeDetailsResponse> result =
          feeSchemePlatformRestClient.getFeeDetails(feeCode);

      // Then
      FeeDetailsResponse categoryOfLawResponse = result.getBody();
      assertThat(categoryOfLawResponse).isNotNull();
      // Check all fields mapped correctly by serializing the result and comparing to expected JSON
      String resultJson = objectMapper.writeValueAsString(categoryOfLawResponse);
      assertThatJsonMatches(expectedBody, resultJson);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 500})
    @DisplayName("Should handle error responses")
    void shouldHandleErrorResponse(int statusCode) throws Exception {
      // Given
      String feeCode = "AB12";

      String expectedBody = readJsonFromFile("fee-scheme/get-fee-details-200.json");

      mockServerClient
          .when(
              HttpRequest.request()
                  .withMethod("GET")
                  .withPath("/api/v0/fee-details/" + feeCode))
          .respond(
              HttpResponse.response()
                  .withStatusCode(statusCode)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      ThrowingCallable result = () -> feeSchemePlatformRestClient.getFeeDetails(feeCode);

      // Then
      HttpStatusCode httpStatusCode = HttpStatusCode.code(statusCode);
      assertThatThrownBy(result)
          .isInstanceOf(WebClientResponseException.class)
          .hasMessageContaining(
              "%s %s from GET %s/api/v0/fee-details/%s"
                  .formatted(
                      httpStatusCode.code(),
                      httpStatusCode.reasonPhrase(),
                      mockServerContainer.getEndpoint(),
                      feeCode));
    }
  }

  @Nested
  @DisplayName("POST /fee-calculation tests")
  class PostFeeCalculationTests {

    @Test
    @DisplayName("Should handle 200 response")
    void shouldHandle200Response() throws Exception {
      // Given
      FeeCalculationRequest feeCalculationRequest = new FeeCalculationRequest();
      String expectedBody = readJsonFromFile("fee-scheme/post-fee-calculation-200.json");

      mockServerClient
          .when(HttpRequest.request().withMethod("POST").withPath("/api/v0/fee-calculation"))
          .respond(
              HttpResponse.response()
                  .withStatusCode(200)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      ResponseEntity<FeeCalculationResponse> result =
          feeSchemePlatformRestClient.calculateFee(feeCalculationRequest);

      // Then
      FeeCalculationResponse feeCalculationResponse = result.getBody();
      assertThat(feeCalculationResponse).isNotNull();
      // Check all fields mapped correctly by serializing the result and comparing to expected JSON
      String resultJson = objectMapper.writeValueAsString(feeCalculationResponse);
      assertThatJsonMatches(expectedBody, resultJson);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 500})
    @DisplayName("Should handle error responses")
    void shouldHandleErrorResponses(int statusCode) throws Exception {
      // Given
      FeeCalculationRequest feeCalculationRequest = new FeeCalculationRequest();
      String expectedBody = readJsonFromFile("fee-scheme/post-fee-calculation-200.json");

      mockServerClient
          .when(HttpRequest.request().withMethod("POST").withPath("/api/v0/fee-calculation"))
          .respond(
              HttpResponse.response()
                  .withStatusCode(statusCode)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      ThrowingCallable result =
          () -> feeSchemePlatformRestClient.calculateFee(feeCalculationRequest);

      // Then
      HttpStatusCode httpStatusCode = HttpStatusCode.code(statusCode);
      assertThatThrownBy(result)
          .isInstanceOf(WebClientResponseException.class)
          .hasMessageContaining(
              "%s %s from POST %s/api/v0/fee-calculation"
                  .formatted(
                      httpStatusCode.code(),
                      httpStatusCode.reasonPhrase(),
                      mockServerContainer.getEndpoint()));
    }
  }
}
