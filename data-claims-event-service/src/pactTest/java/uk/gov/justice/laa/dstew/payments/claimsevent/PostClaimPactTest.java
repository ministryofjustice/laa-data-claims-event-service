package uk.gov.justice.laa.dstew.payments.claimsevent;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.CreateClaim201Response;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1237"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1237") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("POST: /api/v0/submissions/{}/claims PACT tests")
public final class PostClaimPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact postClaim201(PactDslWithProvider builder) {
    // Defines expected 201 response for successfully submitting a valid claim.
    return builder
        .given("the system is ready to process a valid claim")
        .uponReceiving("a new claim request")
        .matchPath("/api/v0/submissions/(" + UUID_REGEX + ")/claims")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("POST")
        .body(objectMapper.writeValueAsString(getClaimPost()))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(201)
        .headers(Map.of("Content-Type", "application/json"))
        .body(
            LambdaDsl.newJsonBody(
                    body ->
                        body.uuid("id", UUID.fromString("d4e3fa24-7d1f-4710-b7a7-0debe88421aa")))
                .build())
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact postClaim400(PactDslWithProvider builder) {
    // Defines expected 400 response for uploading invalid claim
    return builder
        .given("the claim request contains invalid data")
        .uponReceiving("a request to create a claim with invalid data")
        .matchPath("/api/v0/submissions/(" + UUID_REGEX + ")/claims")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("POST")
        .body(objectMapper.writeValueAsString(getClaimPost()))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(400)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @Test
  @DisplayName("Verify 201 response")
  @PactTestFor(pactMethod = "postClaim201")
  void verify201Response() {
    ClaimPost claimPost = getClaimPost();

    ResponseEntity<CreateClaim201Response> response =
        dataClaimsRestClient.createClaim(submissionId.toString(), claimPost);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getId()).isEqualTo(claimId);
  }

  private ClaimPost getClaimPost() {
    return new ClaimPost()
        .id(claimId.toString())
        .submissionId(submissionId.toString())
        .createdByUserId("test-user")
        .lineNumber(1)
        .matterTypeCode("ABC")
        .status(ClaimStatus.READY_TO_PROCESS);
  }

  @Test
  @DisplayName("Verify 400 response")
  @PactTestFor(pactMethod = "postClaim400")
  void verify400Response() {
    ClaimPost claimPost = getClaimPost();

    assertThrows(
        BadRequest.class,
        () -> dataClaimsRestClient.createClaim(submissionId.toString(), claimPost));
  }
}
