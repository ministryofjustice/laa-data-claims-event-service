package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;

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
  private static final List<String> offices = List.of("office1");
  private static final AreaOfLaw areaOfLaw = AreaOfLaw.LEGAL_HELP;
  private static final String submissionPeriod = "2025-07";

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
        .isEqualTo(UUID.fromString("0561d67b-30ed-412e-8231-f6296a53538d"));
  }
}
