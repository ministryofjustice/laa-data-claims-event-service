package uk.gov.justice.laa.dstew.payments.claimsevent.dataclaimsapi;

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
        .matchPath(
            "/api/v1/submissions/(" + UUID_REGEX + ")/claims/(" + UUID_REGEX + ")",
            "/api/v1/submissions/" + EXAMPLE_UUID + "/claims/" + EXAMPLE_UUID)
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX, EXAMPLE_AUTH_TOKEN)
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
    // Defines expected 400 response for uploading invalid claim data (general failure)
    return builder
        .given("the claim patch contains invalid data")
        .uponReceiving("a request to patch a submission with invalid data")
        .matchPath(
            "/api/v1/submissions/(" + UUID_REGEX + ")/claims/(" + UUID_REGEX + ")",
            "/api/v1/submissions/" + EXAMPLE_UUID + "/claims/" + EXAMPLE_UUID)
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX, EXAMPLE_AUTH_TOKEN)
        .method("PATCH")
        .body(objectMapper.writeValueAsString(getClaimPatch()))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(400)
        .matchHeader("Content-Type", "application/(problem\\+)?json", "application/problem+json")
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact patchClaimMissingVersion400(PactDslWithProvider builder) {
    // Defines expected 400 response explicitly for a MISSING version (Jira 1751)
    return builder
        .given("the system is ready to update a claim") // Use valid state so it fails purely on
        // validation
        .uponReceiving("a patch claim request with a missing version")
        .matchPath(
            "/api/v1/submissions/(" + UUID_REGEX + ")/claims/(" + UUID_REGEX + ")",
            "/api/v1/submissions/" + EXAMPLE_UUID + "/claims/" + EXAMPLE_UUID)
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX, EXAMPLE_AUTH_TOKEN)
        .method("PATCH")
        .body(
            objectMapper.writeValueAsString(
                getClaimPatchWithoutVersion())) // Uses the payload with NO version
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(400)
        .matchHeader("Content-Type", "application/(problem\\+)?json", "application/problem+json")
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

  @Test
  @DisplayName("Verify 400 response for general invalid data")
  @PactTestFor(pactMethod = "patchClaim400")
  void verify400Response() {
    ClaimPatch patch = getClaimPatch();
    assertThrows(
        BadRequest.class, () -> dataClaimsRestClient.updateClaim(SUBMISSION_ID, CLAIM_ID, patch));
  }

  @Test
  @DisplayName("Verify 400 response when claim version is explicitly missing")
  @PactTestFor(pactMethod = "patchClaimMissingVersion400")
  void verify400ResponseMissingVersion() {
    ClaimPatch patch = getClaimPatchWithoutVersion();
    assertThrows(
        BadRequest.class, () -> dataClaimsRestClient.updateClaim(SUBMISSION_ID, CLAIM_ID, patch));
  }

  // --- Helper Methods ---

  private ClaimPatch getClaimPatch() {
    return new ClaimPatch()
        .id(CLAIM_ID.toString())
        .status(ClaimStatus.VALID)
        .version(1L) // <--- FIXED: Added mandatory version so valid tests pass!
        .validationMessages(List.of(new ValidationMessagePatch()));
  }

  private ClaimPatch getClaimPatchWithoutVersion() {
    return new ClaimPatch()
        .id(CLAIM_ID.toString())
        .status(ClaimStatus.VALID)
        // Intentionally omitting the .version() property to test validation failure
        .validationMessages(List.of(new ValidationMessagePatch()));
  }
}
