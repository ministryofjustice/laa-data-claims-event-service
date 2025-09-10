package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ExtendWith(MockitoExtension.class)
class DuplicateClaimValidationServiceTest {

  @Mock DataClaimsRestClient dataClaimsRestClient;

  @Mock SubmissionValidationContext submissionValidationContext;

  @InjectMocks DuplicateClaimValidationService duplicateClaimValidationService;

  @Nested
  @DisplayName("validateDuplicateClaims")
  class ValidateDuplicateClaimsTests {

    @Test
    @DisplayName("Crime claims - successful validation does not update context")
    void crimeClaimSuccessfulValidation() {
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

      when(submissionValidationContext.isFlaggedForRetry("claimId1")).thenReturn(false);
      when(dataClaimsRestClient.getClaims(any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(Collections.emptyList())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "CRIME_LOWER", "officeCode");

      // Then
      verify(submissionValidationContext, times(1)).isFlaggedForRetry("claimId1");
      verifyNoMoreInteractions(submissionValidationContext);
    }

    @Test
    @DisplayName(
        "Crime claims - different fee code but the same unique file number passes validation")
    void crimeClaimDifferentFeeCodeButSameUfnPassesValidation() {
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

      when(submissionValidationContext.isFlaggedForRetry("claimId1")).thenReturn(false);
      when(dataClaimsRestClient.getClaims(any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(Collections.emptyList())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "CRIME_LOWER", "officeCode");

      // Then
      verify(submissionValidationContext, times(1)).isFlaggedForRetry("claimId1");
      verifyNoMoreInteractions(submissionValidationContext);
    }

    @Test
    @DisplayName(
        "Crime claims - different unique file number but the same fee code passes validation")
    void crimeClaimDifferentUfnButSameFeeCodePassesValidation() {
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

      when(submissionValidationContext.isFlaggedForRetry("claimId1")).thenReturn(false);
      when(dataClaimsRestClient.getClaims(any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(Collections.emptyList())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "CRIME_LOWER", "officeCode");

      // Then
      verify(submissionValidationContext, times(1)).isFlaggedForRetry("claimId1");
      verifyNoMoreInteractions(submissionValidationContext);
    }

    @Test
    @DisplayName(
        "Crime claims - duplicate claims in submission results in claim error added to validation "
            + "context")
    void crimeClaimDuplicateInSubmissionResultsInClaimErrorAddedToContext() {
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

      when(submissionValidationContext.isFlaggedForRetry("claimId1")).thenReturn(false);
      when(dataClaimsRestClient.getClaims(any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(Collections.emptyList())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "CRIME_LOWER", "officeCode");

      // Then
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claimId1", ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_SUBMISSION);
    }

    @Test
    @DisplayName("Crime claims - duplicate validation ignores invalid claims")
    void crimeClaimDuplicateValidationIgnoresInvalidClaims() {
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
              .status(ClaimStatus.INVALID);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      when(submissionValidationContext.isFlaggedForRetry("claimId1")).thenReturn(false);
      when(dataClaimsRestClient.getClaims(any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(Collections.emptyList())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "CRIME_LOWER", "officeCode");

      // Then
      verify(submissionValidationContext, times(1)).isFlaggedForRetry("claimId1");
      verifyNoMoreInteractions(submissionValidationContext);
    }

    @Test
    @DisplayName(
        "Crime claims - duplicate claims in another submission results in claim error added to "
            + "validation context")
    void crimeClaimDuplicateInAnotherSubmissionResultsInClaimErrorAddedToContext() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);

      ClaimResponse otherClaim =
          new ClaimResponse()
              .id("claimId2")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.VALID);

      List<ClaimResponse> submissionClaims = List.of(claim1);

      when(submissionValidationContext.isFlaggedForRetry("claimId1")).thenReturn(false);
      when(dataClaimsRestClient.getClaims(any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(List.of(otherClaim))));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "CRIME_LOWER", "officeCode");

      // Then
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claimId1", ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    }

    @Test
    @DisplayName("Crime claims - does not reprocess submission claims")
    void crimeClaimDuplicateDoesNotReprocessSubmissionClaims() {
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
      ClaimResponse otherClaim =
          new ClaimResponse()
              .id("claimId2")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      when(submissionValidationContext.isFlaggedForRetry("claimId1")).thenReturn(false);
      when(dataClaimsRestClient.getClaims(any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(List.of(otherClaim))));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "CRIME_LOWER", "officeCode");

      // Then
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claimId1", ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_SUBMISSION);
      verifyNoMoreInteractions(submissionValidationContext);
    }
  }
}
