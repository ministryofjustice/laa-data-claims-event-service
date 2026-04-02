package uk.gov.justice.laa.dstew.payments.claimsevent.feeschemeplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.laa.dstew.payments.claimsevent.feeschemeplatform.AbstractPactTest.PROVIDER;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;
import uk.gov.justice.laa.dstew.payments.claimsevent.feeschemeplatform.model.FeeCalculationRequestProvider;
import uk.gov.justice.laa.dstew.payments.claimsevent.feeschemeplatform.model.FeeCalculationResponseProvider;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "laa.fee-scheme-platform-api.url=http://localhost:1234",
      "laa.fee-scheme-platform-api.accessToken=test-token"
    })
@PactConsumerTest
@PactTestFor(providerName = PROVIDER)
@MockServerConfig(port = "1234")
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("POST: /api/v1/fee-calculation - Pact Tests")
public class PostFeeCalculationV1PactTest extends AbstractPactTest {

  private static final String FEE_CODE = "X123";

  @Autowired FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact postFeeCalculationV1200(PactDslWithProvider builder) {

    return builder
        .given("Fee Calculation POST")
        .uponReceiving("a request to fetch fee calculation response")
        .path("/api/v1/fee-calculation")
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("POST")
        .body(
            objectMapper.writeValueAsString(
                FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID)))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(
            objectMapper.writeValueAsString(
                FeeCalculationResponseProvider.getFeeCalculationResponse(FEE_CODE, CLAIM_ID)))
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact postFeeCalculationV1400(PactDslWithProvider builder) {

    return builder
        .given("Fee Calculation POST")
        .uponReceiving("a request to fetch fee calculation response")
        .path("/api/v1/fee-calculation")
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("POST")
        .body(
            objectMapper.writeValueAsString(
                FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID)))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(400)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact postFeeCalculationV1401(PactDslWithProvider builder) {

    return builder
        .given("Fee Calculation POST")
        .uponReceiving("a request to fetch fee calculation response")
        .path("/api/v1/fee-calculation")
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("POST")
        .body(
            objectMapper.writeValueAsString(
                FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID)))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(401)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact postFeeCalculationV1403(PactDslWithProvider builder) {

    return builder
        .given("Fee Calculation POST")
        .uponReceiving("a request to fetch fee calculation response")
        .path("/api/v1/fee-calculation")
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("POST")
        .body(
            objectMapper.writeValueAsString(
                FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID)))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(403)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact postFeeCalculationV1404(PactDslWithProvider builder) {

    return builder
        .given("Fee Calculation POST")
        .uponReceiving("a request to fetch fee calculation response")
        .path("/api/v1/fee-calculation")
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("POST")
        .body(
            objectMapper.writeValueAsString(
                FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID)))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(404)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact postFeeCalculationV1500(PactDslWithProvider builder) {

    return builder
        .given("Fee Calculation POST")
        .uponReceiving("a request to fetch fee calculation response")
        .path("/api/v1/fee-calculation")
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("POST")
        .body(
            objectMapper.writeValueAsString(
                FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID)))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(500)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  // Validation Tests

  @Test
  @DisplayName("Verify 200 response for v2 fee details")
  @PactTestFor(pactMethod = "postFeeCalculationV1200")
  void verify200Response() {
    var response =
        feeSchemePlatformRestClient.calculateFee(
            FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID));

    assertThat(response).isNotNull();
    assertThat(response.getBody().getFeeCode()).isNotBlank();
    assertThat(response.getBody().getValidationMessages()).isNotEmpty();
  }

  @Test
  @DisplayName("Verify 400 for bad request")
  @PactTestFor(pactMethod = "postFeeCalculationV1400")
  void verify400Response() {
    assertThrows(
        WebClientResponseException.BadRequest.class,
        () ->
            feeSchemePlatformRestClient.calculateFee(
                FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID)));
  }

  @Test
  @DisplayName("Verify 401 for unauthorized")
  @PactTestFor(pactMethod = "postFeeCalculationV1401")
  void verify401Response() {
    assertThrows(
        WebClientResponseException.Unauthorized.class,
        () ->
            feeSchemePlatformRestClient.calculateFee(
                FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID)));
  }

  @Test
  @DisplayName("Verify 403 for forbidden")
  @PactTestFor(pactMethod = "postFeeCalculationV1403")
  void verify403Response() {
    assertThrows(
        WebClientResponseException.Forbidden.class,
        () ->
            feeSchemePlatformRestClient.calculateFee(
                FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID)));
  }

  @Test
  @DisplayName("Verify 404 for missing v1 fee details")
  @PactTestFor(pactMethod = "postFeeCalculationV1404")
  void verify404Response() {
    assertThrows(
        WebClientResponseException.NotFound.class,
        () ->
            feeSchemePlatformRestClient.calculateFee(
                FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID)));
  }

  @Test
  @DisplayName("Verify 500 for Internal server error")
  @PactTestFor(pactMethod = "postFeeCalculationV1500")
  void verify500Response() {
    assertThrows(
        WebClientResponseException.InternalServerError.class,
        () ->
            feeSchemePlatformRestClient.calculateFee(
                FeeCalculationRequestProvider.getFeeCalculationRequest(FEE_CODE, CLAIM_ID)));
  }
}
