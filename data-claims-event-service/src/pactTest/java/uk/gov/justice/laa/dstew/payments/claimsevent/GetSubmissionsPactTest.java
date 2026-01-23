package uk.gov.justice.laa.dstew.payments.claimsevent;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionsResultSet;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1242"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1242") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("GET: /api/v1/submissions PACT tests")
public final class GetSubmissionsPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getSubmissions200(PactDslWithProvider builder) {
    // Defines expected 200 response for submission search
    return builder
        .given("a submission exists for the search criteria")
        .uponReceiving("a search request for submissions")
        .path("/api/v1/submissions")
        .matchQuery("offices", "([A-Z0-9]{6})")
        .matchQuery("area_of_law", "(LEGAL_HELP|CRIME_LOWER|MEDIATION)")
        .queryMatchingDate("submission_period", "MMM-2025")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(
            LambdaDsl.newJsonBody(
                    body -> {
                      body.minArrayLike(
                          "content",
                          1,
                          submission -> {
                            submission.uuid("submission_id", SUBMISSION_ID);
                            submission.uuid("bulk_submission_id", BULK_SUBMISSION_ID);
                            submission.stringType("office_account_number", "string");
                            submission.stringType("submission_period", "string");
                            submission.stringType("area_of_law", "CRIME LOWER");
                            submission.stringType("provider_user_id", "string");
                            submission.stringType("status", "CREATED");
                            submission.uuid("previous_submission_id", UUID.randomUUID());
                            submission.booleanType("is_nil_submission", true);
                            submission.numberType("number_of_claims", 0);
                            submission.datetime("submitted", "yyyy-MM-dd'T'HH:mm:ssXXX");
                            submission.stringType("created_by_user_id", "string");
                          });
                      body.numberType("total_pages", 1);
                      body.numberType("total_elements", 1);
                      body.numberType("number", 0);
                      body.numberType("size", 10);
                    })
                .build())
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getSubmissionsEmpty200(PactDslWithProvider builder) {
    // Defines expected 200 response for submission search, even when empty
    return builder
        .given("no submission exists")
        .uponReceiving("a search request for submissions that returns no results")
        .path("/api/v1/submissions")
        .matchQuery("offices", "([A-Z0-9]{6})")
        .matchQuery("area_of_law", "(LEGAL_HELP|CRIME_LOWER|MEDIATION)")
        .queryMatchingDate("submission_period", "MMM-2025")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(
            LambdaDsl.newJsonBody(
                    body -> {
                      body.array("content", array -> {});
                      body.numberType("total_pages", 0);
                      body.numberType("total_elements", 0);
                      body.numberType("number", 0);
                      body.numberType("size", 10);
                    })
                .build())
        .toPact();
  }

  @Test
  @DisplayName("Verify 200 response")
  @PactTestFor(pactMethod = "getSubmissions200")
  void verify200Response() {
    SubmissionsResultSet submission =
        dataClaimsRestClient
            .getSubmissions(USER_OFFICES, AreaOfLaw.LEGAL_HELP, "APR-2025")
            .getBody();

    assertThat(submission.getContent().size()).isEqualTo(1);
  }

  @Test
  @DisplayName("Verify 200 response empty")
  @PactTestFor(pactMethod = "getSubmissionsEmpty200")
  void verify200ResponseEmpty() {
    SubmissionsResultSet submission =
        dataClaimsRestClient
            .getSubmissions(USER_OFFICES, AreaOfLaw.LEGAL_HELP, "APR-2025")
            .getBody();

    assertThat(submission.getContent().isEmpty()).isTrue();
  }
}
