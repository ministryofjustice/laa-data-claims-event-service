package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.AbstractDuplicateClaimValidatorStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ExtendWith(MockitoExtension.class)
class DuplicatePreviousClaimCivilValidationServiceStrategyTest
    extends AbstractDuplicateClaimValidatorStrategy {

  @Mock private DataClaimsRestClient mockDataClaimsRestClient;

  private DuplicatePreviousClaimCivilValidationServiceStrategy duplicateClaimCivilValidation;

  @BeforeEach
  void beforeEach() {
    duplicateClaimCivilValidation =
        new DuplicatePreviousClaimCivilValidationServiceStrategy(mockDataClaimsRestClient);
  }

  @Nested
  class ValidClaim {

    @DisplayName("Validation error: duplicate disbursement claim on the same submission")
    @Test
    void whenDuplicateDisbursementClaim() {
      var claimTobeProcessed =
          createClaim("claimId1", "DISB01", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var otherClaim =
          createClaim("claimId2", "DISB01", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var submissionClaims = List.of(claimTobeProcessed, otherClaim);
      var context = new SubmissionValidationContext();

      duplicateClaimCivilValidation.validateDuplicateClaims(
          claimTobeProcessed, submissionClaims, "2Q286D", context);

      assertThat(context.hasErrors()).isTrue();

      assertThat(context.getClaimReports()).extracting("claimId").contains("claimId2");

      context
          .getClaimReport("claimId2")
          .ifPresent(
              claimValidationReport ->
                  assertThat(claimValidationReport.getMessages())
                      .extracting("displayMessage")
                      .isEqualTo(
                          List.of(
                              ClaimValidationError
                                  .INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION
                                  .getDisplayMessage())));
    }
  }

  @Nested
  class InvalidClaim {

    @DisplayName(
        "Validation error: When there an exist a civil claim with the same Office, UFN, Fee Code,"
            + " and UCN in the same submission")
    @Test
    void whenExistingClaim() {
      var claimTobeProcessed =
          createClaim("claimId1", "CIV123", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var otherClaim =
          createClaim("claimId2", "CIV123", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var otherClaim1 =
          createClaim("claimId3", "CIV123", "070722/001", "CLI001", ClaimStatus.VALID);
      var submissionClaims = List.of(claimTobeProcessed, otherClaim, otherClaim1);
      var context = new SubmissionValidationContext();

      duplicateClaimCivilValidation.validateDuplicateClaims(
          claimTobeProcessed, submissionClaims, "2Q286D", context);

      assertThat(context.hasErrors()).isTrue();

      assertThat(context.getClaimReports())
          .extracting("claimId")
          .containsAll(List.of("claimId2", "claimId3"));

      context
          .getClaimReport("claimId2")
          .ifPresent(
              claimValidationReport ->
                  assertThat(claimValidationReport.getMessages())
                      .extracting("displayMessage")
                      .isEqualTo(
                          List.of(
                              ClaimValidationError
                                  .INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION
                                  .getDisplayMessage())));
    }
  }
}
