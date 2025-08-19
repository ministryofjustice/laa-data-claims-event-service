package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Optional;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.feescheme.model.CategoryOfLawResponse;
import uk.gov.justice.laa.feescheme.model.FeeCalculationRequest;
import uk.gov.justice.laa.feescheme.model.FeeCalculationResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeeSchemePlatformRestClientIntegrationTest extends MockServerIntegrationTest {

  private FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  @BeforeEach
  void setUp() {
    // Configure WebClient and integrate it with FeeSchemePlatformRestClient
    feeSchemePlatformRestClient = createClient(FeeSchemePlatformRestClient.class);
  }

  @Nested
  @DisplayName("GET: /category-of-law/{feeCode} tests")
  class GetCategoryOfLawTests {

    @Test
    @DisplayName("Should handle 200 response")
    void shouldHandle200Responses() throws Exception {
      // Given
      String feeCode = "AB12";

      String expectedBody = readJsonFromFile("fee-scheme/get-category-of-law-200.json");

      mockServerClient
          .when(HttpRequest.request().withMethod("GET").withPath("/category-of-law/" + feeCode))
          .respond(
              HttpResponse.response()
                  .withStatusCode(200)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      Mono<CategoryOfLawResponse> result = feeSchemePlatformRestClient.getCategoryOfLaw(feeCode);

      // Then
      Optional<CategoryOfLawResponse> categoryOfLawResponse = result.blockOptional();
      assertThat(categoryOfLawResponse).isPresent();
      // Check all fields mapped correctly by serializing the result and comparing to expected JSON
      String resultJson = objectMapper.writeValueAsString(categoryOfLawResponse.get());
      assertThatJsonMatches(expectedBody, resultJson);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 500})
    @DisplayName("Should handle error responses")
    void shouldHandleErrorResponse(int statusCode) throws Exception {
      // Given
      String feeCode = "AB12";

      String expectedBody = readJsonFromFile("fee-scheme/get-category-of-law-200.json");

      mockServerClient
          .when(HttpRequest.request().withMethod("GET").withPath("/category-of-law/" + feeCode))
          .respond(
              HttpResponse.response()
                  .withStatusCode(statusCode)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      Mono<CategoryOfLawResponse> result = feeSchemePlatformRestClient.getCategoryOfLaw(feeCode);

      HttpStatusCode httpStatusCode = HttpStatusCode.code(statusCode);

      // Then
      assertThatThrownBy(result::block)
          .isInstanceOf(WebClientResponseException.class)
          .hasMessageContaining(
              "%s %s from GET %s/category-of-law/%s"
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
          .when(HttpRequest.request().withMethod("POST").withPath("/fee-calculation"))
          .respond(
              HttpResponse.response()
                  .withStatusCode(200)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      Mono<FeeCalculationResponse> result =
          feeSchemePlatformRestClient.calculateFee(feeCalculationRequest);

      // Then
      Optional<FeeCalculationResponse> feeCalculationResponse = result.blockOptional();
      assertThat(feeCalculationResponse).isPresent();
      // Check all fields mapped correctly by serializing the result and comparing to expected JSON
      String resultJson = objectMapper.writeValueAsString(feeCalculationResponse.get());
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
          .when(HttpRequest.request().withMethod("POST").withPath("/fee-calculation"))
          .respond(
              HttpResponse.response()
                  .withStatusCode(statusCode)
                  .withHeader("Content-Type", "application/json")
                  .withBody(expectedBody));

      // When
      Mono<FeeCalculationResponse> result =
          feeSchemePlatformRestClient.calculateFee(feeCalculationRequest);

      HttpStatusCode httpStatusCode = HttpStatusCode.code(statusCode);

      // Then
      assertThatThrownBy(result::block)
          .isInstanceOf(WebClientResponseException.class)
          .hasMessageContaining(
              "%s %s from POST %s/fee-calculation"
                  .formatted(
                      httpStatusCode.code(),
                      httpStatusCode.reasonPhrase(),
                      mockServerContainer.getEndpoint()));
    }
  }
}
