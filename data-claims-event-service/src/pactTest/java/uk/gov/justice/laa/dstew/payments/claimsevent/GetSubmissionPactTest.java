package uk.gov.justice.laa.dstew.payments.claimsevent;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import au.com.dius.pact.consumer.dsl.DslPart;
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
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1235"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1235") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("GET: /api/v1/submissions/{} PACT tests")
public final class GetSubmissionPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getSubmission200(PactDslWithProvider builder) {
    // Defines expected 200 response for existing submission using matchers
    return builder
        .given("a submission exists")
        .uponReceiving("a request to fetch a specific submission")
        .matchPath("/api/v1/submissions/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(getSubmissionResponse())
        .toPact();
  }

  private static DslPart getSubmissionResponse() {
    return LambdaDsl.newJsonBody(
            body -> {
              body.uuid("submission_id", SUBMISSION_ID);
              body.uuid("bulk_submission_id", BULK_SUBMISSION_ID);
              body.stringType("office_account_number", "string");
              body.stringType("submission_period", "string");
              body.stringType("area_of_law", "CRIME LOWER");
              body.stringType("provider_user_id", "string");
              body.stringType("status", "CREATED");
              body.uuid("previous_submission_id", UUID.randomUUID());
              body.booleanType("is_nil_submission", true);
              body.numberType("number_of_claims", 0);
              body.numberType("calculated_total_amount", 0);
              body.datetime("submitted", "yyyy-MM-dd'T'HH:mm:ssXXX");
              body.stringType("created_by_user_id", "string");
              body.minArrayLike(
                  "claims",
                  1,
                  claim -> {
                    claim.uuid("claim_id", CLAIM_ID);
                    claim.stringType("status", "READY_TO_PROCESS");
                  });
            })
        .build();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getSubmission404(PactDslWithProvider builder) {
    // Defines expected 404 response for missing submission
    return builder
        .given("no submission exists")
        .uponReceiving("a request to fetch a non-existent submission")
        .matchPath("/api/v1/submissions/(" + UUID_REGEX + ")")
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @Test
  @DisplayName("Verify 200 response")
  @PactTestFor(pactMethod = "getSubmission200")
  void verify200Response() {
    SubmissionResponse submission = dataClaimsRestClient.getSubmission(SUBMISSION_ID).getBody();

    assertThat(submission).isNotNull();
    assertThat(submission.getSubmissionId()).isEqualTo(SUBMISSION_ID);
  }

  @Test
  @DisplayName("Verify 404 response")
  @PactTestFor(pactMethod = "getSubmission404")
  void verify404Response() {
    assertThrows(NotFound.class, () -> dataClaimsRestClient.getSubmission(SUBMISSION_ID).getBody());
  }
}
