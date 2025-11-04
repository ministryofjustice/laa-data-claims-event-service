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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.AbstractDuplicateClaimValidatorStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ExtendWith(MockitoExtension.class)
class DuplicatePreviousClaimLegalHelpValidationServiceStrategyTest
    extends AbstractDuplicateClaimValidatorStrategy {

  @Mock private DataClaimsRestClient mockDataClaimsRestClient;

  private DuplicatePreviousClaimLegalHelpValidationServiceStrategy
      duplicateClaimLegalHelpValidation;

  @BeforeEach
  void beforeEach() {
    duplicateClaimLegalHelpValidation =
        new DuplicatePreviousClaimLegalHelpValidationServiceStrategy(mockDataClaimsRestClient);
  }

  @Nested
  class ValidClaim {

    @DisplayName("Validation error: duplicate disbursement claim on the same submission")
    @Test
    void whenDuplicateDisbursementClaim() {
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "submissionId1",
              "DISB01",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS);
      var otherClaim =
          createClaim(
              "claimId2",
              "submissionId1",
              "DISB01",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS);
      var submissionClaims = List.of(claimTobeProcessed, otherClaim);
      var context = new SubmissionValidationContext();

      duplicateClaimLegalHelpValidation.validateDuplicateClaims(
          claimTobeProcessed,
          submissionClaims,
          "2Q286D",
          context,
          FeeCalculationType.FIXED.toString());

      assertThat(context.hasErrors()).isTrue();

      assertThat(context.getClaimReports())
          .extracting("claimId")
          .contains(claimTobeProcessed.getId());

      ClaimValidationReport report =
          context.getClaimReport(claimTobeProcessed.getId()).orElseThrow();
      assertThat(report.getMessages())
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .containsExactly(
              ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION
                  .getDisplayMessage());
    }
  }

  @Nested
  class InvalidClaim {

    @DisplayName(
        "Validation error: When there an exist a legal help claim with the same Office, UFN, Fee Code,"
            + " and UCN in the same submission")
    @Test
    void whenExistingClaim() {
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "submissionId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS);
      var otherClaim =
          createClaim(
              "claimId2",
              "submissionId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS);
      var otherClaim1 =
          createClaim(
              "claimId3", "submissionId1", "CIV123", "070722/001", "CLI001", ClaimStatus.VALID);
      var submissionClaims = List.of(claimTobeProcessed, otherClaim, otherClaim1);
      var context = new SubmissionValidationContext();

      duplicateClaimLegalHelpValidation.validateDuplicateClaims(
          claimTobeProcessed,
          submissionClaims,
          "2Q286D",
          context,
          FeeCalculationType.FIXED.toString());

      assertThat(context.hasErrors()).isTrue();

      assertThat(context.getClaimReports())
          .extracting("claimId")
          .contains(claimTobeProcessed.getId());

      ClaimValidationReport report =
          context.getClaimReport(claimTobeProcessed.getId()).orElseThrow();
      assertThat(report.getMessages())
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .containsExactly(
              ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION
                  .getDisplayMessage());
    }
  }
}
