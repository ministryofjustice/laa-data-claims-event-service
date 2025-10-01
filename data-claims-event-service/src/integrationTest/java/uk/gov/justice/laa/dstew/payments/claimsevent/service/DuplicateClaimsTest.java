package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;

@ActiveProfiles("test")
@Component
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.cloud.aws.sqs.enabled=false", // Disable AWS SQS functionality
      "laa.bulk-claim-queue.name=not-used", // Dummy queue name to avoid initialization issues
    })
public class DuplicateClaimsTest extends MockServerIntegrationTest {

  private static final String OFFICE_CODE = "AQ2B3C";
  private static final String AREA_OF_LAW = "CIVIL";
  private static final String FEE_CODE = "432EC";
  private static final String UNIQUE_FILE_NUMBER = "060925/010";
  private static final String UNIQUE_CLIENT_NUMBER = "29384";
  private static final String CLAIM_ID = "6850db96-fbb7-4859-a5a5-fc111cd205b2";
  private static final List<String> SUBMISSION_STATUS =
      List.of("CREATED", "VALIDATION_IN_PROGRESS", "READY_FOR_VALIDATION");
  private static final List<String> CLAIM_STATUSES = List.of("READY_TO_PROCESS", "VALID");
  public static final UUID SUBMISSION_ID = UUID.fromString("00000000-0000-0001-0000-000000000001");

  @Autowired protected SubmissionValidationService submissionValidationService;

  @Nested
  class CivilDuplicateClaimValidation {

    @DisplayName("Civil Duplicate Claim Validation: Should not have any validation errors")
    @Test
    void testCivilDuplicateClaimValidation() throws Exception {
      UUID submissionId = new UUID(1, 1);

      // submission
      stubForGetSubmission(submissionId, "data-claims/get-submission/get-submission-civil.json");

      stubForPathSubmissionWithClaimsId(submissionId, CLAIM_ID);

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
          List.of(new Parameter("areaOfLaw", AREA_OF_LAW)),
          "provider-details/get-firm-schedules-openapi-200.json");
      // fee-details
      stubForGteFeeDetails(FEE_CODE, "fee-scheme/get-fee-details-200.json");
      // fee-calculation
      stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");
      // Stub patch submission
      mockUpdateSubmission204(SUBMISSION_ID);

      var actualValidationContext = submissionValidationService.validateSubmission(submissionId);

      Assertions.assertTrue(actualValidationContext.getSubmissionValidationErrors().isEmpty());
    }
  }
}
