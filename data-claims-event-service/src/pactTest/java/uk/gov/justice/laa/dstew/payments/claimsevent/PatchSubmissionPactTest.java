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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1240"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1240") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("PATCH: /api/v0/submissions PACT tests")
public final class PatchSubmissionPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact patchSubmissionStatusOnly(PactDslWithProvider builder) {
    // Defines expected 204 response for successfully updating a submission
    return builder
        .given("the system is ready to update a submission")
        .uponReceiving("a patch submission request updating status only")
        .matchPath("/api/v0/submissions/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("PATCH")
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(204)
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact patchSubmission400(PactDslWithProvider builder) {
    // Defines expected 400 response for uploading invalid submission
    return builder
        .given("the submission patch contains invalid data")
        .uponReceiving("a request to patch a submission with invalid data")
        .matchPath("/api/v0/submissions/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("PATCH")
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(400)
        .toPact();
  }

  @Test
  @DisplayName("Verify 204 response for patch submission status only")
  @PactTestFor(pactMethod = "patchSubmissionStatusOnly")
  void verify204Response() {
    SubmissionPatch submissionPatch =
        new SubmissionPatch()
            .submissionId(submissionId)
            .status(SubmissionStatus.VALIDATION_SUCCEEDED);

    ResponseEntity<Void> response =
        dataClaimsRestClient.updateSubmission(submissionId.toString(), submissionPatch);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  @DisplayName("Verify 400 response")
  @PactTestFor(pactMethod = "patchSubmission400")
  void verify400Response() {
    SubmissionPatch submissionPatch =
        new SubmissionPatch()
            .submissionId(submissionId)
            .status(SubmissionStatus.VALIDATION_SUCCEEDED);
    assertThrows(
        BadRequest.class,
        () -> dataClaimsRestClient.updateSubmission(submissionId.toString(), submissionPatch));
  }
}
