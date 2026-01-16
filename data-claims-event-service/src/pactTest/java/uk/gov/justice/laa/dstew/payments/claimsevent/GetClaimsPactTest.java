package uk.gov.justice.laa.dstew.payments.claimsevent;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1243"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1243") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("GET: /api/v0/claims PACT tests")
public final class GetClaimsPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact getClaims200(PactDslWithProvider builder) {
    String claimsResponse = readJsonFromFile("get-claims-200.json");
    // Defines expected 200 response for claims response
    return builder
        .given("claims exist for the search criteria")
        .uponReceiving("a request to search for claims")
        .path("/api/v0/claims")
        .matchQuery("submission_id", UUID_REGEX)
        .matchQuery("office_code", "([A-Z0-9]{6})")
        .matchQuery("submission_statuses", ANY_FORMAT_REGEX)
        .matchQuery("fee_code", ANY_FORMAT_REGEX)
        .matchQuery("unique_file_number", ANY_FORMAT_REGEX)
        .matchQuery("unique_client_number", ANY_FORMAT_REGEX)
        .matchQuery("unique_case_id", ANY_FORMAT_REGEX)
        .matchQuery("claim_statuses", ANY_FORMAT_REGEX)
        .matchQuery("page", ANY_NUMBER_REGEX)
        .matchQuery("size", ANY_NUMBER_REGEX)
        .matchQuery("sort", "(asc|desc)")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(claimsResponse)
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact getClaimsEmpty200(PactDslWithProvider builder) {
    String clamsResponse = readJsonFromFile("get-empty-search-200.json");
    // Defines expected 200 response for claims response, even when empty
    return builder
        .given("no claims exist for the search criteria")
        .uponReceiving("a request to search for claims with no results")
        .path("/api/v0/claims")
        .matchQuery("office_code", "([A-Z0-9]{6})")
        .matchQuery("submission_id", UUID_REGEX)
        .matchQuery("office_code", "([A-Z0-9]{6})")
        .matchQuery("submission_statuses", ANY_FORMAT_REGEX)
        .matchQuery("fee_code", ANY_FORMAT_REGEX)
        .matchQuery("unique_file_number", ANY_FORMAT_REGEX)
        .matchQuery("unique_client_number", ANY_FORMAT_REGEX)
        .matchQuery("unique_case_id", ANY_FORMAT_REGEX)
        .matchQuery("claim_statuses", ANY_FORMAT_REGEX)
        .matchQuery("page", ANY_NUMBER_REGEX)
        .matchQuery("size", ANY_NUMBER_REGEX)
        .matchQuery("sort", "(asc|desc)")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(clamsResponse)
        .toPact();
  }

  @Test
  @DisplayName("Verify 200 response")
  @PactTestFor(pactMethod = "getClaims200")
  void verify200Response() {
    ClaimResultSet claims =
        dataClaimsRestClient
            .getClaims(
                userOffices.get(0),
                submissionId.toString(),
                List.of(SubmissionStatus.VALIDATION_SUCCEEDED),
                "FEE_10",
                "UFN",
                "UCN",
                "case_id",
                List.of(ClaimStatus.VALID),
                0,
                10,
                "asc")
            .getBody();

    assertThat(claims.getContent().size()).isEqualTo(1);
  }

  @Test
  @DisplayName("Verify 200 response empty")
  @PactTestFor(pactMethod = "getClaimsEmpty200")
  void verify200ResponseEmpty() {
    ClaimResultSet claims =
        dataClaimsRestClient
            .getClaims(
                userOffices.get(0),
                submissionId.toString(),
                List.of(SubmissionStatus.VALIDATION_SUCCEEDED),
                "FEE_10",
                "UFN",
                "UCN",
                "case_id",
                List.of(ClaimStatus.VALID),
                0,
                10,
                "asc")
            .getBody();

    assertThat(claims.getContent().isEmpty()).isTrue();
  }
}
