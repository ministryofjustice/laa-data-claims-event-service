package uk.gov.justice.laa.dstew.payments.claimsevent.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
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
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SubmissionValidationService;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ActiveProfiles("test")
@ImportTestcontainers(MessageListenerBase.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(MockServerIntegrationTest.ClaimsConfiguration.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.cloud.aws.sqs.enabled=false", "laa.bulk-claim-queue.name=not-used"})
@DisplayName("High-level incident integration tests for provider-details cache behaviour")
public class SubmissionValidationServiceIncidentIntegrationTest extends MockServerIntegrationTest {

  @Autowired protected SubmissionValidationService submissionValidationService;

  @Autowired
  protected uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient
      dataClaimsRestClient;

  private static final UUID SUBMISSION_ID = UUID.fromString("0561d67b-30ed-412e-8231-f6296a53538d");
  private static final UUID BULK_SUBMISSION_ID =
      UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
  private static final String OFFICE_CODE = "AQ2B3C";

  @Test
  void incidentShapeShouldResolveLaterCrimeClaimToCrimeSchedule() throws Exception {
    // Submission contains two claims: an earlier Civil-dated claim and a later Crime-dated claim.
    stubForGetSubmission(SUBMISSION_ID, "data-claims/get-submission/get-submission-incident.json");
    stubForUpdateSubmission(SUBMISSION_ID);
    // Ensure PATCH updates for individual claims are stubbed to return 204
    stubForUpdateClaim(SUBMISSION_ID, UUID.fromString("11111111-1111-1111-1111-111111111111"));
    stubForUpdateClaim(SUBMISSION_ID, UUID.fromString("22222222-2222-2222-2222-222222222222"));
    stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

    // Return two claims for the submission: first Civil (2025-06-01), then Crime (2026-07-01)
    stubForGetClaims(Collections.emptyList(), "data-claims/get-claims/incident-two-claims.json");

    // Provider details: for Civil effective date return an open-ended Civil schedule
    stubForGetProviderOffice(
        OFFICE_CODE,
        List.of(new Parameter("effectiveDate", "01-06-2025")),
        "provider-details/incident-civil.json");

    // For the later Crime date return a Crime schedule
    stubForGetProviderOffice(
        OFFICE_CODE,
        List.of(new Parameter("effectiveDate", "01-07-2026")),
        "provider-details/incident-crime.json");

    // Fee calculation and fee details can be stubbed to return nominal successful responses
    stubForGetFeeDetails("CAPA", "fee-scheme/get-fee-details-200.json");
    stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");

    // Ensure duplicate-submission check (queries /api/v1/submissions) returns no results for this
    // test case so the submission-level duplicate validator does not cause a remote 502 from
    // MockServer when no expectation is registered.
    getStubForGetSubmissionByCriteria(
        List.of(
            Parameter.param("offices", OFFICE_CODE),
            Parameter.param("area_of_law", AreaOfLaw.LEGAL_HELP.name()),
            Parameter.param("submission_period", "APR-2025")),
        "data-claims/get-submission/get-submissions-by-filter_no_content.json");

    // When
    // Sanity-check the deserialised submission returned by the data-claims stub
    var submission = dataClaimsRestClient.getSubmission(SUBMISSION_ID).getBody();

    SubmissionValidationContext context =
        submissionValidationService.validateSubmission(SUBMISSION_ID);

    // Then: expect no validation errors for the later Crime claim (i.e., it used its Crime
    // schedule)
    assertThat(context.getSubmissionValidationErrors()).isEmpty();
  }
}
