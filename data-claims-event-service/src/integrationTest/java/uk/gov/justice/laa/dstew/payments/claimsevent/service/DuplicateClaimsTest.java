package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
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
public class DuplicateClaimsTest extends MockServerIntegrationTest {

  private static final String OFFICE_CODE = "AQ2B3C";
  private static final String FEE_CODE = "432EC";
  private static final String SUBMISSION_PERIOD = "APR-2025";
  private static final String CLAIM_ID = "6850db96-fbb7-4859-a5a5-fc111cd205b2";
  public static final UUID SUBMISSION_ID = UUID.fromString("00000000-0000-0001-0000-000000000001");
  public static final UUID BULK_SUBMISSION_ID =
      UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

  @Autowired protected SubmissionValidationService submissionValidationService;

  @Nested
  class LegalHelpDuplicateClaimValidation {

    @DisplayName("Legal Help Duplicate Claim Validation: Should not have any validation errors")
    @Test
    void testLegalHelpDuplicateClaimValidation() throws Exception {
      // submission
      stubForGetSubmission(
          SUBMISSION_ID, "data-claims/get-submission/get-submission-legal-help.json");

      stubForPathSubmissionWithClaimsId(SUBMISSION_ID, CLAIM_ID);

      // claims
      stubForGetClaim(
          SUBMISSION_ID,
          UUID.fromString("f6bde766-a0a3-483b-bf13-bef888b4f06e"),
          "data-claims/get-claim/get-claim-1.json");
      stubForGetClaim(
          SUBMISSION_ID,
          UUID.fromString("6850db96-fbb7-4859-a5a5-fc111cd205b2"),
          "data-claims/get-claim/get-claim-1.json");

      stubForGetClaims(Collections.emptyList(), "data-claims/get-claims/no-claims.json");
      // provider-details
      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(new Parameter("areaOfLaw", AreaOfLaw.LEGAL_HELP.getValue())),
          "provider-details/get-firm-schedules-openapi-200.json");
      // fee-details
      stubForGetFeeDetails(FEE_CODE, "fee-scheme/get-fee-details-200.json");
      // fee-calculation
      stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");
      // Stub patch submission
      stubForUpdateSubmission(SUBMISSION_ID);
      // Stub patch bulk submission
      stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);

      getStubForGetSubmissionByCriteria(
          List.of(
              Parameter.param("offices", OFFICE_CODE),
              Parameter.param("area_of_law", AreaOfLaw.LEGAL_HELP.name()),
              Parameter.param("submission_period", SUBMISSION_PERIOD)),
          "data-claims/get-submission/get-submissions-by-filter_no_content.json");

      var actualValidationContext = submissionValidationService.validateSubmission(SUBMISSION_ID);

      Assertions.assertTrue(actualValidationContext.getSubmissionValidationErrors().isEmpty());
    }
  }
}
