package uk.gov.justice.laa.dstew.payments.claimsevent;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.List;
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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1241"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1241") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("PATCH: /api/v1/submissions/{}/claims/{} PACT tests")
public final class PatchClaimPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact patchClaimStatus(PactDslWithProvider builder) {
    // Defines expected 204 response for successfully updating a claim
    return builder
        .given("the system is ready to update a claim")
        .uponReceiving("a patch claim request updating status only")
        .matchPath("/api/v1/submissions/(" + UUID_REGEX + ")/claims/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("PATCH")
        .body(objectMapper.writeValueAsString(getClaimPatch()))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(204)
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact patchClaim400(PactDslWithProvider builder) {
    // Defines expected 400 response for uploading invalid claim
    return builder
        .given("the claim patch contains invalid data")
        .uponReceiving("a request to patch a submission with invalid data")
        .matchPath("/api/v1/submissions/(" + UUID_REGEX + ")/claims/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("PATCH")
        .body(objectMapper.writeValueAsString(getClaimPatch()))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(400)
        .toPact();
  }

  @Test
  @DisplayName("Verify 204 response for patch claim status only")
  @PactTestFor(pactMethod = "patchClaimStatus")
  void verify204Response() {
    ClaimPatch patch = getClaimPatch();

    ResponseEntity<Void> response =
        dataClaimsRestClient.updateClaim(SUBMISSION_ID, CLAIM_ID, patch);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  private ClaimPatch getClaimPatch() {
    return new ClaimPatch()
        .id(CLAIM_ID.toString())
        .status(ClaimStatus.VALID)
        .validationMessages(List.of(new ValidationMessagePatch()));
  }

  @Test
  @DisplayName("Verify 400 response")
  @PactTestFor(pactMethod = "patchClaim400")
  void verify400Response() {
    ClaimPatch patch = getClaimPatch();
    assertThrows(
        BadRequest.class, () -> dataClaimsRestClient.updateClaim(SUBMISSION_ID, CLAIM_ID, patch));
  }
}
