package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.*;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.FeeCalculationPatchMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bulk claim updater test")
class BulkClaimUpdaterTest {

  @Mock DataClaimsRestClient dataClaimsRestClient;
  @Mock EventServiceMetricService mockEventServiceMetricService;
  @Mock FeeCalculationPatchMapper mockFeeCalculationPatchMapper;
  @Mock FeeCalculationService mockFeeCalculationService;

  @InjectMocks BulkClaimUpdater bulkClaimUpdater;

  @Captor private ArgumentCaptor<ClaimPatch> claimPatchCaptor;
  @Captor private ArgumentCaptor<UUID> submissionIdCaptor;
  @Captor private ArgumentCaptor<UUID> claimIdCaptor;

  private final UUID SUBMISSION_ID = new UUID(1, 0);
  private final UUID CLAIM_ID_ONE = new UUID(1, 1);
  private final UUID CLAIM_ID_TWO = new UUID(1, 2);
  private static final String FEE_CODE = "feeCode1";
  private final SubmissionValidationContext context = new SubmissionValidationContext();

  @Test
  @DisplayName("Should not update any claims when no claims added")
  void shouldNotUpdateAnyClaimsWhenNoClaimsAdded() {
    // Given

    Map<String, FeeDetailsResponseWrapper> feeDetailsResponseWrapperHashMap =
        buildFeeDetailsResponseWrapperHashMap();
    // When
    bulkClaimUpdater.updateClaims(
        SUBMISSION_ID,
        Collections.emptyList(),
        AreaOfLaw.LEGAL_HELP,
        context,
        feeDetailsResponseWrapperHashMap);
    // Then
    verify(dataClaimsRestClient, never()).updateClaim(any(), any(), any());
    verify(mockFeeCalculationService, never()).calculateFee(any(), any(), any());
    verify(mockEventServiceMetricService, never()).startFspValidationTimer(any());
    verify(mockEventServiceMetricService, never()).stopFspValidationTimer(any());
    verify(mockFeeCalculationPatchMapper, never()).mapToFeeCalculationPatch(any(), any());
  }

  @Test
  @DisplayName("Should update one claim to valid when no errors")
  void shouldUpdateOneClaimToValidWhenNoErrors() {
    // Given
    var claimResponse = buildClaimResponse(CLAIM_ID_ONE);
    var feeDetailsResponseWrapperHashMap = buildFeeDetailsResponseWrapperHashMap();
    var feeCalculationResponse = new FeeCalculationResponse();

    when(mockFeeCalculationService.calculateFee(
            eq(claimResponse), eq(context), eq(AreaOfLaw.LEGAL_HELP)))
        .thenReturn(Optional.of(feeCalculationResponse));

    var feeCalculationPatch =
        new FeeCalculationPatch().claimId(UUID.fromString(claimResponse.getId()));
    when(mockFeeCalculationPatchMapper.mapToFeeCalculationPatch(
            feeCalculationResponse,
            feeDetailsResponseWrapperHashMap
                .get(claimResponse.getFeeCode())
                .getFeeDetailsResponse()))
        .thenReturn(feeCalculationPatch);

    // When
    bulkClaimUpdater.updateClaims(
        SUBMISSION_ID,
        List.of(claimResponse),
        AreaOfLaw.LEGAL_HELP,
        context,
        feeDetailsResponseWrapperHashMap);
    // Then
    verify(mockEventServiceMetricService).startFspValidationTimer(claimIdCaptor.capture());
    verify(mockEventServiceMetricService).stopFspValidationTimer(claimIdCaptor.capture());
    verify(mockFeeCalculationService)
        .calculateFee(eq(claimResponse), eq(context), eq(AreaOfLaw.LEGAL_HELP));
    verify(dataClaimsRestClient, times(1))
        .updateClaim(
            submissionIdCaptor.capture(), claimIdCaptor.capture(), claimPatchCaptor.capture());
    ClaimPatch capturedPatch = claimPatchCaptor.getValue();
    assertThat(submissionIdCaptor.getValue()).isEqualTo(SUBMISSION_ID);
    assertThat(claimIdCaptor.getValue()).isEqualTo(UUID.fromString(claimResponse.getId()));
    assertThat(capturedPatch.getId()).isEqualTo(claimResponse.getId());
    assertThat(capturedPatch.getFeeCalculationResponse()).isEqualTo(feeCalculationPatch);
    assertThat(capturedPatch.getValidationMessages().isEmpty()).isTrue();
    assertThat(capturedPatch.getStatus()).isEqualTo(ClaimStatus.VALID);
  }

  @Test
  @DisplayName("Should update a claim when fee calculation service returns null response")
  void shouldUpdateClaimsWithNullFeeResponse() {
    // Given
    var claimResponse = buildClaimResponse(CLAIM_ID_ONE);
    var feeDetailsResponseWrapperHashMap = buildFeeDetailsResponseWrapperHashMap();

    when(mockFeeCalculationService.calculateFee(
            eq(claimResponse), eq(context), eq(AreaOfLaw.LEGAL_HELP)))
        .thenReturn(Optional.empty());

    var feeCalculationPatch =
        new FeeCalculationPatch().claimId(UUID.fromString(claimResponse.getId()));
    when(mockFeeCalculationPatchMapper.mapToFeeCalculationPatch(
            null,
            feeDetailsResponseWrapperHashMap
                .get(claimResponse.getFeeCode())
                .getFeeDetailsResponse()))
        .thenReturn(feeCalculationPatch);

    // When
    bulkClaimUpdater.updateClaims(
        SUBMISSION_ID,
        List.of(claimResponse),
        AreaOfLaw.LEGAL_HELP,
        context,
        feeDetailsResponseWrapperHashMap);
    // Then
    verify(mockEventServiceMetricService).startFspValidationTimer(claimIdCaptor.capture());
    verify(mockEventServiceMetricService).stopFspValidationTimer(claimIdCaptor.capture());
    verify(mockFeeCalculationService)
        .calculateFee(eq(claimResponse), eq(context), eq(AreaOfLaw.LEGAL_HELP));
    verify(dataClaimsRestClient, times(1))
        .updateClaim(
            submissionIdCaptor.capture(), claimIdCaptor.capture(), claimPatchCaptor.capture());
    ClaimPatch capturedPatch = claimPatchCaptor.getValue();
    assertThat(submissionIdCaptor.getValue()).isEqualTo(SUBMISSION_ID);
    assertThat(claimIdCaptor.getValue()).isEqualTo(UUID.fromString(claimResponse.getId()));
    assertThat(capturedPatch.getId()).isEqualTo(claimResponse.getId());
    assertThat(capturedPatch.getFeeCalculationResponse()).isEqualTo(feeCalculationPatch);
    assertThat(capturedPatch.getValidationMessages().isEmpty()).isTrue();
    assertThat(capturedPatch.getStatus()).isEqualTo(ClaimStatus.VALID);
  }

  @Test
  @DisplayName("Should update two claims to valid when no errors")
  void shouldUpdateTwoClaimToValidWhenNoErrors() {
    // Given
    var claimResponseOne = buildClaimResponse(CLAIM_ID_ONE);
    var claimResponseTwo = buildClaimResponse(CLAIM_ID_TWO);
    var feeDetailsResponseWrapperHashMap = buildFeeDetailsResponseWrapperHashMap();
    var feeCalculationResponseOne = new FeeCalculationResponse().claimId(CLAIM_ID_ONE.toString());
    SubmissionValidationContext context = new SubmissionValidationContext();
    when(mockFeeCalculationService.calculateFee(
            any(ClaimResponse.class), any(SubmissionValidationContext.class), any(AreaOfLaw.class)))
        .thenReturn(Optional.of(feeCalculationResponseOne));

    new FeeCalculationPatch().claimId(UUID.fromString(claimResponseOne.getId()));
    var feeCalculationPatchTwo =
        new FeeCalculationPatch().claimId(UUID.fromString(claimResponseTwo.getId()));

    when(mockFeeCalculationPatchMapper.mapToFeeCalculationPatch(
            any(FeeCalculationResponse.class), any(FeeDetailsResponse.class)))
        .thenReturn(feeCalculationPatchTwo);

    // When
    bulkClaimUpdater.updateClaims(
        SUBMISSION_ID,
        List.of(claimResponseTwo, claimResponseTwo),
        AreaOfLaw.LEGAL_HELP,
        context,
        feeDetailsResponseWrapperHashMap);
    // Then

    verify(dataClaimsRestClient, times(2)).updateClaim(any(), any(), any());
    verify(mockFeeCalculationService, times(2)).calculateFee(any(), any(), any());
    verify(mockFeeCalculationPatchMapper, times(2)).mapToFeeCalculationPatch(any(), any());
    verify(mockEventServiceMetricService, times(2)).startFspValidationTimer(any());
    verify(mockEventServiceMetricService, times(2)).stopFspValidationTimer(any());
  }

  @Test
  @DisplayName("Should update one claim to invalid when one claim has errors in context")
  void shouldUpdateOneClaimToInvalidWhenOtherClaimHasInvalidStatus() {
    // Given
    var validClaimResponse = buildClaimResponse(CLAIM_ID_ONE);
    var invalidClaimResponse = buildClaimResponse(CLAIM_ID_TWO);
    var feeDetailsResponseWrapperHashMap = buildFeeDetailsResponseWrapperHashMap();
    var feeCalculationResponseOne = new FeeCalculationResponse().claimId(CLAIM_ID_ONE.toString());

    when(mockFeeCalculationService.calculateFee(
            eq(validClaimResponse), eq(context), eq(AreaOfLaw.LEGAL_HELP)))
        .thenReturn(Optional.of(feeCalculationResponseOne));

    var feeCalculationPatch =
        new FeeCalculationPatch().claimId(UUID.fromString(validClaimResponse.getId()));
    when(mockFeeCalculationPatchMapper.mapToFeeCalculationPatch(
            feeCalculationResponseOne,
            feeDetailsResponseWrapperHashMap
                .get(validClaimResponse.getFeeCode())
                .getFeeDetailsResponse()))
        .thenReturn(feeCalculationPatch);

    context.addClaimError(
        String.valueOf(invalidClaimResponse.getId()),
        ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    // When
    bulkClaimUpdater.updateClaims(
        SUBMISSION_ID,
        List.of(validClaimResponse, invalidClaimResponse),
        AreaOfLaw.LEGAL_HELP,
        context,
        feeDetailsResponseWrapperHashMap);
    // Then
    // Should skip INVALID claim so only claim two exists
    verify(dataClaimsRestClient, times(2)).updateClaim(any(), any(), claimPatchCaptor.capture());
    ClaimPatch capturedPatch = claimPatchCaptor.getAllValues().getFirst();
    assertThat(capturedPatch.getId()).isEqualTo(validClaimResponse.getId());
    assertThat(capturedPatch.getStatus()).isEqualTo(ClaimStatus.VALID);
    ClaimPatch capturedPatchTwo = claimPatchCaptor.getAllValues().get(1);
    assertThat(capturedPatchTwo.getId()).isEqualTo(invalidClaimResponse.getId());
    assertThat(capturedPatchTwo.getStatus()).isEqualTo(ClaimStatus.INVALID);
    org.hamcrest.MatcherAssert.assertThat(
        capturedPatchTwo.getValidationMessages(),
        org.hamcrest.Matchers.contains(
            org.hamcrest.beans.HasPropertyWithValue.hasProperty(
                "displayMessage",
                org.hamcrest.CoreMatchers.is(
                    ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION
                        .getDisplayMessage()))));
  }

  @Test
  @DisplayName(
      "Should update claim to invalid with validation error messages when context has errors")
  void shouldUpdateClaimToInvalidWhenContextHasErrors() {
    // Given
    var claimResponse = buildClaimResponse(CLAIM_ID_ONE);
    var feeDetailsResponseWrapperHashMap = buildFeeDetailsResponseWrapperHashMap();
    var feeCalculationResponse = new FeeCalculationResponse().claimId(CLAIM_ID_ONE.toString());

    when(mockFeeCalculationService.calculateFee(
            eq(claimResponse), eq(context), eq(AreaOfLaw.LEGAL_HELP)))
        .thenReturn(Optional.of(feeCalculationResponse));

    var feeCalculationPatch =
        new FeeCalculationPatch().claimId(UUID.fromString(claimResponse.getId()));
    when(mockFeeCalculationPatchMapper.mapToFeeCalculationPatch(
            feeCalculationResponse,
            feeDetailsResponseWrapperHashMap
                .get(claimResponse.getFeeCode())
                .getFeeDetailsResponse()))
        .thenReturn(feeCalculationPatch);

    context.addClaimReports(List.of(new ClaimValidationReport(claimResponse.getId())));
    context.flagForRetry(claimResponse.getId());
    context.addClaimError(
        String.valueOf(claimResponse.getId()),
        ClaimValidationError.TECHNICAL_ERROR_FEE_CALCULATION_SERVICE);
    // When
    bulkClaimUpdater.updateClaims(
        SUBMISSION_ID,
        List.of(claimResponse),
        AreaOfLaw.LEGAL_HELP,
        context,
        feeDetailsResponseWrapperHashMap);
    // Then

    // Should skip INVALID claim so only claim two exists
    verify(dataClaimsRestClient, times(1)).updateClaim(any(), any(), claimPatchCaptor.capture());
    ClaimPatch capturedPatch = claimPatchCaptor.getAllValues().getFirst();
    assertThat(capturedPatch.getId()).isEqualTo(claimResponse.getId());
    assertThat(capturedPatch.getStatus()).isEqualTo(ClaimStatus.INVALID);
    assertThat(capturedPatch.getValidationMessages().getFirst().getDisplayMessage())
        .isEqualTo(
            ClaimValidationError.TECHNICAL_ERROR_FEE_CALCULATION_SERVICE.getDisplayMessage());
  }

  @Test
  @DisplayName("Should not update claim to invalid when the claim is flagged for retry")
  void shouldNotUpdateClaimToInvalidWhenTheClaimIsFlaggedForRetry() {
    // Given

    var claimResponse = buildClaimResponse(CLAIM_ID_ONE);
    var feeDetailsResponseWrapperHashMap = buildFeeDetailsResponseWrapperHashMap();

    context.addClaimReports(List.of(new ClaimValidationReport(claimResponse.getId())));
    context.flagForRetry(claimResponse.getId());
    // When
    bulkClaimUpdater.updateClaims(
        SUBMISSION_ID,
        List.of(claimResponse),
        AreaOfLaw.LEGAL_HELP,
        context,
        feeDetailsResponseWrapperHashMap);
    // Then

    // Should skip INVALID claim so only claim two exists
    verify(dataClaimsRestClient, never()).updateClaim(any(), any(), any());
    verify(mockFeeCalculationService, never()).calculateFee(any(), any(), any());
    verify(mockEventServiceMetricService, never()).startFspValidationTimer(any());
    verify(mockEventServiceMetricService, never()).stopFspValidationTimer(any());
    verify(mockFeeCalculationPatchMapper, never()).mapToFeeCalculationPatch(any(), any());
  }

  private static @NotNull Map<String, FeeDetailsResponseWrapper>
      buildFeeDetailsResponseWrapperHashMap() {
    return Map.of(
        FEE_CODE,
        FeeDetailsResponseWrapper.withFeeDetailsResponse(
            new FeeDetailsResponse().feeCodeDescription("feeCodeDescription")));
  }

  private ClaimResponse buildClaimResponse(final UUID uuid) {
    return new ClaimResponse()
        .id(uuid.toString())
        .feeCode(FEE_CODE)
        .status(ClaimStatus.READY_TO_PROCESS)
        .submissionId(SUBMISSION_ID.toString());
  }
}
