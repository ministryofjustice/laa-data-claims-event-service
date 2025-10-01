package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

  @Autowired protected SubmissionValidationService submissionValidationService;

  @DynamicPropertySource
  static void overrideClaimsApi(DynamicPropertyRegistry registry) {

    registry.add("laa.claims-api.hostname", mockStaticServerContainer::getEndpoint);
    registry.add("laa.provider-details-api.hostname", mockStaticServerContainer::getEndpoint);
  }

  @Nested
  class CivilDuplicateClaimValidation {
    @DisplayName("Civil Duplicate Claim Validation: Should not have any validation errors")
    @Test
    void testCivilDuplicateClaimValidation() throws Exception {
      UUID submissionId = new UUID(1, 1);

      // submission
      stubForGetSubmission(submissionId, "data-claims/get-submission-civil.json");

      stubForPathSubmissionWithClaimsId(submissionId, CLAIM_ID);

      // claims
      stubForGetClaims(
          List.of(
              new Parameter("officeCode", OFFICE_CODE),
              new Parameter("submissionId", submissionId.toString())),
          "data-claims/get-claim.json");

      stubForGetClaims(
          List.of(
              new Parameter("uniqueFileNumber", UNIQUE_FILE_NUMBER),
              new Parameter("uniqueClientNumber", UNIQUE_CLIENT_NUMBER),
              new Parameter("submissionStatuses", SUBMISSION_STATUS),
              new Parameter("officeCode", OFFICE_CODE),
              new Parameter("feeCode", FEE_CODE),
              new Parameter("claimStatuses", CLAIM_STATUSES)),
          "data-claims/get-empty-claim.json");

      // provider-details
      stubForGetProviderOffice(
          OFFICE_CODE,
          List.of(new Parameter("areaOfLaw", AREA_OF_LAW)),
          "provider-details/get-firm-schedules-openapi-200.json");
      // fee-details
      stubForGteFeeDetails(FEE_CODE, "fee-scheme/get-fee-details-200.json");
      // fee-calculation
      stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");

      var actualValidationContext = submissionValidationService.validateSubmission(submissionId);

      Assertions.assertTrue(actualValidationContext.getSubmissionValidationErrors().isEmpty());
    }
  }
}
