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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.CreateSubmission201Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1236"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1236") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("POST: /api/v0/submissions PACT tests")
public final class PostSubmissionPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact postSubmission201(PactDslWithProvider builder) {
    String postSubmissionResponse = readJsonFromFile("post-submission-201.json");
    // Defines expected 201 response for successfully submitting valid submission
    return builder
        .given("the system is ready to process a valid submission")
        .uponReceiving("a new submission request")
        .path("/api/v0/submissions")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("POST")
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(201)
        .headers(Map.of("Content-Type", "application/json"))
        .body(postSubmissionResponse)
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact postSubmission400(PactDslWithProvider builder) {
    String postSubmissionResponse = readJsonFromFile("post-submission-201.json");
    // Defines expected 400 response for uploading invalid submission
    return builder
        .given("the submission file contains invalid data")
        .uponReceiving("a request to create a submission with invalid data")
        .path("/api/v0/submissions")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("POST")
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(400)
        .headers(Map.of("Content-Type", "application/json"))
        .body(postSubmissionResponse)
        .toPact();
  }

  @Test
  @DisplayName("Verify 201 response")
  @PactTestFor(pactMethod = "postSubmission201")
  void verify201Response() {
    SubmissionPost submissionPost =
        new SubmissionPost()
            .bulkSubmissionId(bulkSubmissionId)
            .submissionId(submissionId)
            .submissionPeriod("APR-2025")
            .areaOfLaw(AreaOfLaw.LEGAL_HELP)
            .numberOfClaims(2)
            .officeAccountNumber("ABC123");

    ResponseEntity<CreateSubmission201Response> response =
        dataClaimsRestClient.createSubmission(submissionPost);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getId()).isEqualTo(submissionId);
  }

  @Test
  @DisplayName("Verify 400 response")
  @PactTestFor(pactMethod = "postSubmission400")
  void verify400Response() {
    SubmissionPost submissionPost =
        new SubmissionPost()
            .bulkSubmissionId(bulkSubmissionId)
            .submissionId(submissionId)
            .submissionPeriod("APR-2025")
            .areaOfLaw(AreaOfLaw.LEGAL_HELP)
            .numberOfClaims(2)
            .officeAccountNumber("ABC123");
    assertThrows(BadRequest.class, () -> dataClaimsRestClient.createSubmission(submissionPost));
  }
}
