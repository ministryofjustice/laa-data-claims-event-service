package uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate.DuplicateClaimCrimeLowerValidationServiceStrategy;

@ExtendWith(MockitoExtension.class)
class DuplicateClaimCrimeLowerValidationServiceStrategyTest {

  @Mock DataClaimsRestClient dataClaimsRestClient;

  @InjectMocks DuplicateClaimCrimeLowerValidationServiceStrategy duplicateClaimValidationService;

  @Nested
  @DisplayName("validateDuplicateClaims")
  class ValidateDuplicateClaimsTests {

    @Test
    @DisplayName("Crime Lower claims - successful validation does not update context")
    void crimeLowerClaimSuccessfulValidation() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .feeCode("feeCode1")
              .uniqueFileNumber("ufn1")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .feeCode("feeCode2")
              .uniqueFileNumber("ufn2")
              .status(ClaimStatus.READY_TO_PROCESS);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName(
        "Crime Lower claims - different fee code but the same unique file number passes validation")
    void crimeLowerClaimDifferentFeeCodeButSameUfnPassesValidation() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .feeCode("feeCode1")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .feeCode("feeCode2")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName(
        "Crime Lower claims - different unique file number but the same fee code passes validation")
    void crimeLowerClaimDifferentUfnButSameFeeCodePassesValidation() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn1")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn2")
              .status(ClaimStatus.READY_TO_PROCESS);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName(
        "Crime Lower claims - duplicate claims in submission results in claim error added to validation "
            + "context")
    void crimeLowerClaimDuplicateInSubmissionResultsInClaimErrorAddedToContext() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(submissionClaims);

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors(claim1.getId())).isTrue();
      assertContextClaimError(
          context,
          claim1.getId(),
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION);
    }

    @Test
    @DisplayName("Crime Lower claims - duplicate validation ignores invalid claims")
    void crimeLowerClaimDuplicateValidationIgnoresInvalidClaims() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.INVALID);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(submissionClaims);

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(claimResultSet)));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName(
        "Crime Lower claims - duplicate claims in another submission results in claim error added to "
            + "validation context")
    void crimeLowerClaimDuplicateInAnotherSubmissionResultsInClaimErrorAddedToContext() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);

      ClaimResponse otherClaim =
          new ClaimResponse()
              .id("claimId2")
              .submissionId("submissionId2")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.VALID);

      List<ClaimResponse> submissionClaims = List.of(claim1);

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(List.of(otherClaim));

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(claimResultSet)));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors(claim1.getId())).isTrue();
      assertContextClaimError(
          context,
          claim1.getId(),
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    }

    @Test
    @DisplayName("Crime Lower claims - does not reprocess submission claims")
    void crimeLowerClaimDuplicateDoesNotReprocessSubmissionClaims() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse otherClaim =
          new ClaimResponse()
              .id("claimId2")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(List.of(otherClaim));

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(claimResultSet)));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertContextClaimError(
          context,
          claim1.getId(),
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION);
      assertThat(context.hasErrors(claim2.getId())).isFalse();
    }
  }
}
