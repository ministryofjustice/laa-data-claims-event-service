package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.cloud.aws.sqs.enabled=false", // Disable AWS SQS functionality
      "laa.bulk-claim-queue.name=not-used", // Dummy queue name to avoid initialization issues
    })
public class DataClaimRestClientIntTest extends MockServerIntegrationTest {

  @Autowired private DataClaimsRestClient dataClaimsRestClient;
  private static final List<String> offices = List.of("office1");
  private static final String submissionId = "f6bde766-a0a3-483b-bf13-bef888b4f06e";
  private static final LocalDate submittedDateFrom = LocalDate.of(2025, 1, 1);

  private static final LocalDate submittedDateTo = LocalDate.of(2025, 12, 29);
  private static final String areaOfLaw = "CIVIL";
  private static final String submissionPeriod = "2025-07";

  @DisplayName(
      "should return 200 when Data claim rest client is called for submission matching the criteria")
  @Test
  public void shouldReturnListOfSubmissionMatchingTheCriteria() throws Exception {

    getStubForGetSubmissionByCriteria(
        List.of(
            Parameter.param("offices", offices),
            Parameter.param("submission-id", submissionId),
            Parameter.param("submitted-date-from", "01/01/2025"),
            Parameter.param("submitted-date-to", "29/12/2025"),
            Parameter.param("area-of-law", areaOfLaw),
            Parameter.param("submission-period", submissionPeriod)),
        "data-claims/get-submission/get-submissions-by-filter.json");

    var actualResults =
        dataClaimsRestClient.getSubmissions(
            offices,
            submissionId,
            submittedDateFrom,
            submittedDateTo,
            areaOfLaw,
            submissionPeriod,
            0,
            0,
            null);

    assertThat(actualResults.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(actualResults.getBody().getContent().get(0).getSubmissionId())
        .isEqualTo(UUID.fromString("0561d67b-30ed-412e-8231-f6296a53538d"));
  }
}
