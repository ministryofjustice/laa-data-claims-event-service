package uk.gov.justice.laa.dstew.payments.claimsevent;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1234"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1234") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("GET: /api/v0/bulk-submissions/{} PACT tests")
public final class GetBulkSubmissionPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact getBulkSubmission200(PactDslWithProvider builder) {
    String bulkSubmissionResponse = readJsonFromFile("get-bulk-submission-200.json");
    // Defines expected 200 response for existing bulk submission
    return builder
        .given("a bulk submission exists")
        .uponReceiving("a request to fetch a specific bulk submission")
        .matchPath("/api/v0/bulk-submissions/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(bulkSubmissionResponse)
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact getBulkSubmission404(PactDslWithProvider builder) {
    // Defines expected 404 response for missing submission
    return builder
        .given("no submission exists")
        .uponReceiving("a request to fetch a non-existent bulk submission")
        .matchPath("/api/v0/bulk-submissions/(" + UUID_REGEX + ")")
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @Test
  @DisplayName("Verify 200 response")
  @PactTestFor(pactMethod = "getBulkSubmission200")
  void verify200Response() {
    GetBulkSubmission200Response response =
        dataClaimsRestClient.getBulkSubmission(bulkSubmissionId).getBody();

    assertThat(response).isNotNull();
    assertThat(response.getBulkSubmissionId()).isEqualTo(bulkSubmissionId);
  }

  @Test
  @DisplayName("Verify 404 response")
  @PactTestFor(pactMethod = "getBulkSubmission404")
  void verify404Response() {
    assertThrows(
        NotFound.class, () -> dataClaimsRestClient.getBulkSubmission(bulkSubmissionId).getBody());
  }
}
