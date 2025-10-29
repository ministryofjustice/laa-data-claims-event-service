package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionAreaOfLaw;
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
  private static final BulkSubmissionAreaOfLaw areaOfLaw = BulkSubmissionAreaOfLaw.LEGAL_HELP;
  private static final String submissionPeriod = "2025-07";

  @DisplayName(
      """
    GIVEN valid filter criteria and pagination values
    WHEN the DataClaimsRestClient is called to retrieve submissions
    THEN it should return a 200 response with the expected submission data
    """)
  @ParameterizedTest(name = "page={0}, size={1}, sort={2}")
  @CsvSource(
      value = {"0, 20, asc", "1, 10, desc", "2, 50, asc", "null, null, null"},
      nullValues = "null")
  void shouldReturnListOfSubmissionMatchingTheCriteria(Integer page, Integer size, String sort)
      throws Exception {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    LocalDate submittedDateFrom = LocalDate.of(2025, 1, 1);
    LocalDate submittedDateTo = LocalDate.of(2025, 12, 29);

    var params = new java.util.ArrayList<Parameter>();
    params.add(Parameter.param("offices", offices));
    params.add(Parameter.param("submission_id", submissionId));
    params.add(Parameter.param("submitted_date_from", submittedDateFrom.format(formatter)));
    params.add(Parameter.param("submitted_date_to", submittedDateTo.format(formatter)));
    params.add(Parameter.param("area_of_law", String.valueOf(areaOfLaw)));
    params.add(Parameter.param("submission_period", submissionPeriod));

    if (page != null) params.add(Parameter.param("page", page.toString()));
    if (size != null) params.add(Parameter.param("size", size.toString()));
    if (sort != null) params.add(Parameter.param("sort", sort));

    getStubForGetSubmissionByCriteria(
        params, "data-claims/get-submission/get-submissions-by-filter.json");

    var actualResults =
        dataClaimsRestClient.getSubmissions(
            offices,
            submissionId,
            submittedDateFrom,
            submittedDateTo,
            areaOfLaw,
            submissionPeriod,
            page,
            size,
            sort);

    assertThat(actualResults.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actualResults.getBody().getContent().get(0).getSubmissionId())
        .isEqualTo(UUID.fromString("0561d67b-30ed-412e-8231-f6296a53538d"));
  }
}
