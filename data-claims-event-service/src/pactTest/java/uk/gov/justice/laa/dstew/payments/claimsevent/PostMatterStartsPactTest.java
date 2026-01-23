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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.CreateMatterStart201Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.MatterStartPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.MediationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1233"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1233") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("POST: /api/v1/submissions/{}/matter-starts PACT tests")
public final class PostMatterStartsPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact postMatterStart201(PactDslWithProvider builder) {
    // Defines expected 201 response for successfully submitting a valid matter start.
    return builder
        .given("the system is ready to process a valid matter start")
        .uponReceiving("a new matter start request")
        .matchPath("/api/v1/submissions/(" + UUID_REGEX + ")/matter-starts")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("POST")
        .body(objectMapper.writeValueAsString(getMatterStartPost()))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(201)
        .headers(Map.of("Content-Type", "application/json"))
        .body(LambdaDsl.newJsonBody(body -> body.uuid("id", MATTER_START_ID)).build())
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact postMatterStart400(PactDslWithProvider builder) {
    // Defines expected 400 response for uploading invalid matter start
    return builder
        .given("the matter start request contains invalid data")
        .uponReceiving("a request to create a matter start with invalid data")
        .matchPath("/api/v1/submissions/(" + UUID_REGEX + ")/matter-starts")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("POST")
        .body(objectMapper.writeValueAsString(getMatterStartPost()))
        .matchHeader("Content-Type", "application/json")
        .willRespondWith()
        .status(400)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @Test
  @DisplayName("Verify 201 response")
  @PactTestFor(pactMethod = "postMatterStart201")
  void verify201Response() {
    MatterStartPost matterStartPost = getMatterStartPost();

    ResponseEntity<CreateMatterStart201Response> response =
        dataClaimsRestClient.createMatterStart(SUBMISSION_ID.toString(), matterStartPost);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  private static MatterStartPost getMatterStartPost() {
    return new MatterStartPost()
        .mediationType(MediationType.MDAC_ALL_ISSUES_CO)
        .createdByUserId("test-user")
        .accessPointCode("A0001")
        .scheduleReference("SCH123");
  }

  @Test
  @DisplayName("Verify 400 response")
  @PactTestFor(pactMethod = "postMatterStart400")
  void verify400Response() {
    MatterStartPost matterStartPost = getMatterStartPost();

    assertThrows(
        BadRequest.class,
        () -> dataClaimsRestClient.createMatterStart(SUBMISSION_ID.toString(), matterStartPost));
  }
}
