package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.*;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.FeeCalculationPatchMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bulk claim updater test")
class BulkClaimUpdaterTest {

  @Mock DataClaimsRestClient dataClaimsRestClient;
  @Mock EventServiceMetricService mockEventServiceMetricService;
  @Mock FeeCalculationPatchMapper mockFeeCalculationPatchMapper;
  @Mock FeeCalculationService mockFeeCalculationService;

  @InjectMocks BulkClaimUpdater bulkClaimUpdater;

  private final UUID SUBMISSION_ID = new UUID(1, 0);

  @Test
  @DisplayName("Should not update any claims when no claims added")
  void shouldNotUpdateAnyClaimsWhenNoClaimsAdded() {
    // Given
    SubmissionResponse build =
        SubmissionResponse.builder()
            .submissionId(SUBMISSION_ID)
            .claims(Collections.emptyList())
            .build();
    SubmissionValidationContext context = new SubmissionValidationContext();

    FeeDetailsResponseWrapper feeDetailsResponseWrapper =
        FeeDetailsResponseWrapper.withFeeDetailsResponse(
            new FeeDetailsResponse().feeCodeDescription("feeCodeDescription"));

    Map<String, FeeDetailsResponseWrapper> feeDetailsResponseWrapperHashMap = new HashMap<>();
    feeDetailsResponseWrapperHashMap.put("feeCode1", feeDetailsResponseWrapper);
    feeDetailsResponseWrapperHashMap.put("feeCode2", feeDetailsResponseWrapper);
    // When
    bulkClaimUpdater.updateClaims(
        SUBMISSION_ID,
        Collections.emptyList(),
        AreaOfLaw.LEGAL_HELP,
        context,
        feeDetailsResponseWrapperHashMap);
    // Then
    verify(dataClaimsRestClient, times(0)).updateClaim(any(), any(), any());
    verify(mockFeeCalculationService, never()).calculateFee(any(), any(), any());
    verify(mockEventServiceMetricService, never()).startFspValidationTimer(any());
    verify(mockEventServiceMetricService, never()).stopFspValidationTimer(any());
    verify(mockFeeCalculationPatchMapper, never()).mapToFeeCalculationPatch(any(), any());
  }
  /*
  @Test
  @DisplayName("Should update one claim to valid when no errors")
  void shouldUpdateOneClaimToValidWhenNoErrors() {
    // Given
    UUID claimId = new UUID(1, 1);
    SubmissionResponse build =
        SubmissionResponse.builder()
            .submissionId(SUBMISSION_ID)
            .build()
            .addClaimsItem(
                SubmissionClaim.builder()
                    .claimId(claimId)
                    .status(ClaimStatus.READY_TO_PROCESS)
                    .build());
    SubmissionValidationContext context = new SubmissionValidationContext();
    // When
    bulkClaimUpdater.updateClaims(build, context);
    // Then

    ArgumentCaptor<ClaimPatch> claimPatchCaptor = ArgumentCaptor.forClass(ClaimPatch.class);
    verify(dataClaimsRestClient, times(1)).updateClaim(any(), any(), claimPatchCaptor.capture());
    ClaimPatch capturedPatch = claimPatchCaptor.getValue();
    assertThat(capturedPatch.getId()).isEqualTo(claimId.toString());
    assertThat(capturedPatch.getStatus()).isEqualTo(ClaimStatus.VALID);
  }

  @ParameterizedTest
  @EnumSource(
      value = ClaimStatus.class,
      names = {"VALID", "INVALID"})
  @DisplayName("Should not update one claim when status not READY_TO_PROCESS")
  void shouldNotUpdateOneClaimWhenStatusNotReadyToProcess(ClaimStatus claimStatus) {
    // Given
    UUID claimId = new UUID(1, 1);
    SubmissionResponse build =
        SubmissionResponse.builder()
            .submissionId(SUBMISSION_ID)
            .build()
            .addClaimsItem(SubmissionClaim.builder().claimId(claimId).status(claimStatus).build());
    SubmissionValidationContext context = new SubmissionValidationContext();
    // When
    bulkClaimUpdater.updateClaims(build, context);
    // Then

    ArgumentCaptor<ClaimPatch> claimPatchCaptor = ArgumentCaptor.forClass(ClaimPatch.class);
    verify(dataClaimsRestClient, times(0)).updateClaim(any(), any(), claimPatchCaptor.capture());
  }

  @Test
  @DisplayName("Should update two claims to valid when no errors")
  void shouldUpdateTwoClaimToValidWhenNoErrors() {
    // Given
    UUID claimId = new UUID(1, 1);
    UUID claimIdTwo = new UUID(1, 2);
    SubmissionResponse build =
        SubmissionResponse.builder()
            .submissionId(SUBMISSION_ID)
            .build()
            .addClaimsItem(
                SubmissionClaim.builder()
                    .status(ClaimStatus.READY_TO_PROCESS)
                    .claimId(claimId)
                    .build())
            .addClaimsItem(
                SubmissionClaim.builder()
                    .status(ClaimStatus.READY_TO_PROCESS)
                    .claimId(claimIdTwo)
                    .build());

    SubmissionValidationContext context = new SubmissionValidationContext();
    // When
    bulkClaimUpdater.updateClaims(build, context);
    // Then
    ArgumentCaptor<ClaimPatch> claimPatchCaptor = ArgumentCaptor.forClass(ClaimPatch.class);
    verify(dataClaimsRestClient, times(2)).updateClaim(any(), any(), claimPatchCaptor.capture());
    ClaimPatch capturedPatch = claimPatchCaptor.getAllValues().getFirst();
    assertThat(capturedPatch.getId()).isEqualTo(claimId.toString());
    assertThat(capturedPatch.getStatus()).isEqualTo(ClaimStatus.VALID);
    ClaimPatch capturedPatchTwo = claimPatchCaptor.getAllValues().get(1);
    assertThat(capturedPatchTwo.getId()).isEqualTo(claimIdTwo.toString());
    assertThat(capturedPatchTwo.getStatus()).isEqualTo(ClaimStatus.VALID);
  }

  @Test
  @DisplayName("Should update one claim to invalid when one claim has errors in context")
  void shouldUpdateOneClaimToInvalidWhenOtherClaimHasInvalidStatus() {
    // Given
    UUID claimId = new UUID(1, 1);
    UUID claimIdTwo = new UUID(1, 2);
    SubmissionResponse build =
        SubmissionResponse.builder()
            .submissionId(SUBMISSION_ID)
            .build()
            .addClaimsItem(
                SubmissionClaim.builder()
                    .status(ClaimStatus.READY_TO_PROCESS)
                    .claimId(claimId)
                    .build())
            .addClaimsItem(
                SubmissionClaim.builder()
                    .status(ClaimStatus.READY_TO_PROCESS)
                    .claimId(claimIdTwo)
                    .build());

    SubmissionValidationContext context = new SubmissionValidationContext();
    context.addClaimError(
        String.valueOf(claimId),
        ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    // When
    bulkClaimUpdater.updateClaims(build, context);
    // Then
    ArgumentCaptor<ClaimPatch> claimPatchCaptor = ArgumentCaptor.forClass(ClaimPatch.class);
    // Should skip INVALID claim so only claim two exists
    verify(dataClaimsRestClient, times(2)).updateClaim(any(), any(), claimPatchCaptor.capture());
    ClaimPatch capturedPatch = claimPatchCaptor.getAllValues().getFirst();
    assertThat(capturedPatch.getId()).isEqualTo(claimId.toString());
    assertThat(capturedPatch.getStatus()).isEqualTo(ClaimStatus.INVALID);
    ClaimPatch capturedPatchTwo = claimPatchCaptor.getAllValues().get(1);
    assertThat(capturedPatchTwo.getId()).isEqualTo(claimIdTwo.toString());
    assertThat(capturedPatchTwo.getStatus()).isEqualTo(ClaimStatus.VALID);
  }

  @Test
  @DisplayName(
      "Should update claim to invalid with validation error messages when context has errors")
  void shouldUpdateClaimToInvalidWhenContextHasErrors() {
    // Given
    UUID claimId = new UUID(1, 1);
    SubmissionResponse build =
        SubmissionResponse.builder()
            .submissionId(SUBMISSION_ID)
            .build()
            .addClaimsItem(
                SubmissionClaim.builder()
                    .status(ClaimStatus.READY_TO_PROCESS)
                    .claimId(claimId)
                    .build());

    SubmissionValidationContext context = new SubmissionValidationContext();
    context.addClaimReports(List.of(new ClaimValidationReport(claimId.toString())));
    context.flagForRetry(claimId.toString());
    context.addClaimError(
        String.valueOf(claimId), ClaimValidationError.TECHNICAL_ERROR_FEE_CALCULATION_SERVICE);
    // When
    bulkClaimUpdater.updateClaims(build, context);
    // Then
    ArgumentCaptor<ClaimPatch> claimPatchCaptor = ArgumentCaptor.forClass(ClaimPatch.class);
    // Should skip INVALID claim so only claim two exists
    verify(dataClaimsRestClient, times(1)).updateClaim(any(), any(), claimPatchCaptor.capture());
    ClaimPatch capturedPatch = claimPatchCaptor.getAllValues().getFirst();
    assertThat(capturedPatch.getId()).isEqualTo(claimId.toString());
    assertThat(capturedPatch.getStatus()).isEqualTo(ClaimStatus.INVALID);
    assertThat(capturedPatch.getValidationMessages().getFirst().getDisplayMessage())
        .isEqualTo(
            ClaimValidationError.TECHNICAL_ERROR_FEE_CALCULATION_SERVICE.getDisplayMessage());
  }

  @Test
  @DisplayName("Should not update claim to invalid when the claim is flagged for retry")
  void shouldNotUpdateClaimToInvalidWhenTheClaimIsFlaggedForRetry() {
    // Given
    UUID claimId = new UUID(1, 1);
    SubmissionResponse build =
        SubmissionResponse.builder()
            .submissionId(SUBMISSION_ID)
            .build()
            .addClaimsItem(
                SubmissionClaim.builder()
                    .status(ClaimStatus.READY_TO_PROCESS)
                    .claimId(claimId)
                    .build());

    SubmissionValidationContext context = new SubmissionValidationContext();
    context.addClaimReports(List.of(new ClaimValidationReport(claimId.toString())));
    context.flagForRetry(claimId.toString());
    // When
    bulkClaimUpdater.updateClaims(build, context);
    // Then
    ArgumentCaptor<ClaimPatch> claimPatchCaptor = ArgumentCaptor.forClass(ClaimPatch.class);
    // Should skip INVALID claim so only claim two exists
    verify(dataClaimsRestClient, never()).updateClaim(any(), any(), claimPatchCaptor.capture());
  }*/
}
