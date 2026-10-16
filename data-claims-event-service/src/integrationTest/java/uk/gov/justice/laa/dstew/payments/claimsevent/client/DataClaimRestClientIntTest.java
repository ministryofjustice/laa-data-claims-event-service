package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockserver.model.Parameter;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.BulkSubmissionMapper;

@ActiveProfiles("test")
@ImportTestcontainers(MessageListenerBase.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(MockServerIntegrationTest.ClaimsConfiguration.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.cloud.aws.sqs.enabled=false", // Disable AWS SQS functionality
      "laa.bulk-claim-queue.name=not-used", // Dummy queue name to avoid initialization issues
    })
public class DataClaimRestClientIntTest extends MockServerIntegrationTest {

  @Autowired private DataClaimsRestClient dataClaimsRestClient;
  @Autowired private BulkSubmissionMapper bulkSubmissionMapper;
  private static final List<String> offices = List.of("office1");
  private static final AreaOfLaw areaOfLaw = AreaOfLaw.LEGAL_HELP;
  private static final String submissionPeriod = "2025-07";

  @ParameterizedTest
  @EnumSource(AreaOfLaw.class)
  @DisplayName("Should send mapped Inquest details in the claim creation request")
  void shouldSendMappedInquestDetailsToClaimsApi(AreaOfLaw claimAreaOfLaw) throws Exception {
    String submissionId = UUID.randomUUID().toString();
    String path = "/api/v1/submissions/" + submissionId + "/claims";
    List<String> departments =
        List.of(
            "Ministry of Justice", "Ministry of Justice", "Department for Health and Social Care");
    BulkSubmissionOutcome outcome =
        new BulkSubmissionOutcome()
            .isClientMeansTested(false)
            .deceasedForename("John")
            .deceasedSurname("Smith")
            .deceasedDateOfDeath("01/01/2025")
            .coronersInquestReference("INQ-TEST-001")
            .interestedDepartments(departments);
    mockServerClient
        .when(request().withMethod("POST").withPath(path))
        .respond(response().withStatusCode(201));

    var claim = bulkSubmissionMapper.mapToClaimPost(outcome, claimAreaOfLaw);
    var result = dataClaimsRestClient.createClaim(submissionId, claim);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    var expectedRequest = request().withMethod("POST").withPath(path);
    mockServerClient.verify(expectedRequest, VerificationTimes.exactly(1));
    var recordedRequests = mockServerClient.retrieveRecordedRequests(expectedRequest);
    JsonNode requestBody = objectMapper.readTree(recordedRequests[0].getBodyAsString());
    JsonNode inquestDetail = requestBody.path("inquest_detail");
    assertThat(inquestDetail.path("is_client_means_tested"))
        .isEqualTo(objectMapper.valueToTree(false));
    assertThat(inquestDetail.path("deceased_forename").asText()).isEqualTo("John");
    assertThat(inquestDetail.path("deceased_surname").asText()).isEqualTo("Smith");
    assertThat(inquestDetail.path("deceased_date_of_death").asText()).isEqualTo("01/01/2025");
    assertThat(inquestDetail.path("coroners_inquest_reference").asText()).isEqualTo("INQ-TEST-001");
    assertThat(inquestDetail.path("interested_departments"))
        .isEqualTo(objectMapper.valueToTree(departments));
  }

  @DisplayName(
      """
    GIVEN valid filter criteria and pagination values
    WHEN the DataClaimsRestClient is called to retrieve submissions
    THEN it should return a 200 response with the expected submission data
    """)
  @Test
  void shouldReturnListOfSubmissionMatchingTheCriteria() throws Exception {

    var params = new java.util.ArrayList<Parameter>();
    params.add(Parameter.param("offices", offices));
    params.add(Parameter.param("area_of_law", areaOfLaw.name()));
    params.add(Parameter.param("submission_period", submissionPeriod));

    getStubForGetSubmissionByCriteria(
        params, "data-claims/get-submission/get-submissions-by-filter.json");

    var actualResults = dataClaimsRestClient.getSubmissions(offices, areaOfLaw, submissionPeriod);

    assertThat(actualResults.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actualResults.getBody().getContent().get(0).getSubmissionId())
        .isEqualTo(EXISTING_DUPLICATE_SUBMISSION_ID);
  }
}
