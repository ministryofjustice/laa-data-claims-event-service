package uk.gov.justice.laa.dstew.payments.claimsevent;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1241"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1241") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("PATCH: /api/v1/bulk-submissions PACT tests")
public final class PatchBulkSubmissionPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact patchBulkSubmissionStatusOnly(PactDslWithProvider builder) {
    // Defines expected 204 response for successfully updating a bulk submission
    return builder
        .given("the system is ready to update a bulk submission")
        .uponReceiving("a patch bulk submission request updating status only")
        .matchPath("/api/v1/bulk-submissions/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("PATCH")
        .body(objectMapper.writeValueAsString(getBulkSubmissionPatch()))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(204)
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact patchBulkSubmission400(PactDslWithProvider builder) {
    // Defines expected 400 response for uploading invalid bulk submission
    return builder
        .given("the bulk submission patch contains invalid data")
        .uponReceiving("a request to patch a bulk submission with invalid data")
        .matchPath("/api/v1/bulk-submissions/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("PATCH")
        .body(objectMapper.writeValueAsString(getBulkSubmissionPatch()))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(400)
        .toPact();
  }

  @Test
  @DisplayName("Verify 204 response for patch bulk submission status only")
  @PactTestFor(pactMethod = "patchBulkSubmissionStatusOnly")
  void verify204Response() {
    BulkSubmissionPatch patch = getBulkSubmissionPatch();

    ResponseEntity<Void> response =
        dataClaimsRestClient.updateBulkSubmission(BULK_SUBMISSION_ID.toString(), patch);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  @DisplayName("Verify 400 response")
  @PactTestFor(pactMethod = "patchBulkSubmission400")
  void verify400Response() {
    BulkSubmissionPatch patch = getBulkSubmissionPatch();
    assertThrows(
        BadRequest.class,
        () -> dataClaimsRestClient.updateBulkSubmission(BULK_SUBMISSION_ID.toString(), patch));
  }

  private BulkSubmissionPatch getBulkSubmissionPatch() {
    return new BulkSubmissionPatch()
        .bulkSubmissionId(BULK_SUBMISSION_ID)
        .status(BulkSubmissionStatus.PARSING_COMPLETED);
  }
}
