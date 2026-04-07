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
@DisplayName("GET: /api/v2/fee-details/{feeCode} - Pact Tests")
public class GetFeeDetailsV2PactTest extends AbstractPactTest {

  private static final String FEE_CODE = "X123";

  @Autowired FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getFeeDetailsV2200(PactDslWithProvider builder) {

    return builder
        .given("fee details exist for the fee code")
        .uponReceiving("a request to fetch fee details (v2)")
        .path("/api/v2/fee-details/" + FEE_CODE)
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(
            """
            {
            "categoryOfLawCodes": ["IMM"],
            "feeCodeDescription": "Fee Description",
            "feeType": "HOURLY"
            }
            """)
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getFeeDetailsV2400(PactDslWithProvider builder) {

    return builder
        .given("Bad request")
        .uponReceiving("a request to fetch fee details (v2)")
        .path("/api/v2/fee-details/" + FEE_CODE)
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("GET")
        .willRespondWith()
        .status(400)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getFeeDetailsV2401(PactDslWithProvider builder) {

    return builder
        .given("Unauthorized")
        .uponReceiving("a request to fetch fee details (v2)")
        .path("/api/v2/fee-details/" + FEE_CODE)
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("GET")
        .willRespondWith()
        .status(401)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getFeeDetailsV2403(PactDslWithProvider builder) {

    return builder
        .given("Forbidden")
        .uponReceiving("a request to fetch fee details (v2)")
        .path("/api/v2/fee-details/" + FEE_CODE)
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("GET")
        .willRespondWith()
        .status(403)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getFeeDetailsV2404(PactDslWithProvider builder) {

    return builder
        .given("Category code not found")
        .uponReceiving("a request to fetch fee details (v2)")
        .path("/api/v2/fee-details/" + FEE_CODE)
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(Map.of("Content-Type", "application/json"))
        .body(
            """
            {
              "timestamp": "2025-09-08T13:15:30",
              "status": 404,
              "error": "Not Found",
              "message": "Category code not found for fee code: X123"
            }
            """)
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getFeeDetailsV2500(PactDslWithProvider builder) {

    return builder
        .given("Internal server error")
        .uponReceiving("a request to fetch fee details (v2)")
        .path("/api/v2/fee-details/" + FEE_CODE)
        .matchHeader(HttpHeaders.AUTHORIZATION, ".+", "test-token")
        .method("GET")
        .willRespondWith()
        .status(500)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  // Validation Tests

  @Test
  @DisplayName("Verify 200 response for v2 fee details")
  @PactTestFor(pactMethod = "getFeeDetailsV2200")
  void verify200Response() {
    var response = feeSchemePlatformRestClient.getFeeDetails(FEE_CODE).getBody();

    assertThat(response).isNotNull();
    assertThat(response.getFeeCodeDescription()).isNotBlank();
    assertThat(response.getCategoryOfLawCodes()).isNotEmpty();
  }

  @Test
  @DisplayName("Verify 400 for bad request")
  @PactTestFor(pactMethod = "getFeeDetailsV2400")
  void verify400Response() {
    assertThrows(
        WebClientResponseException.BadRequest.class,
        () -> feeSchemePlatformRestClient.getFeeDetails(FEE_CODE));
  }

  @Test
  @DisplayName("Verify 401 for unauthorized")
  @PactTestFor(pactMethod = "getFeeDetailsV2401")
  void verify401Response() {
    assertThrows(
        WebClientResponseException.Unauthorized.class,
        () -> feeSchemePlatformRestClient.getFeeDetails(FEE_CODE));
  }

  @Test
  @DisplayName("Verify 403 for forbidden")
  @PactTestFor(pactMethod = "getFeeDetailsV2403")
  void verify403Response() {
    assertThrows(
        WebClientResponseException.Forbidden.class,
        () -> feeSchemePlatformRestClient.getFeeDetails(FEE_CODE));
  }

  @Test
  @DisplayName("Verify 404 for missing v2 fee details")
  @PactTestFor(pactMethod = "getFeeDetailsV2404")
  void verify404Response() {

    WebClientResponseException.NotFound notFound =
        assertThrows(
            WebClientResponseException.NotFound.class,
            () -> feeSchemePlatformRestClient.getFeeDetails(FEE_CODE));
    assertThat(notFound.getResponseBodyAsString())
        .contains("Category code not found for fee code:");
  }

  @Test
  @DisplayName("Verify 500 for Internal server error")
  @PactTestFor(pactMethod = "getFeeDetailsV2500")
  void verify500Response() {
    assertThrows(
        WebClientResponseException.InternalServerError.class,
        () -> feeSchemePlatformRestClient.getFeeDetails(FEE_CODE));
  }
}
