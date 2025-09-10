package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.FeeSchemeMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationRequest;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;
import uk.gov.justice.laa.fee.scheme.model.Warning;

@ExtendWith(MockitoExtension.class)
class FeeCalculationServiceTest {

  @Mock private SubmissionValidationContext validationContext;

  @Mock private FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  @Mock private FeeSchemeMapper feeSchemeMapper;

  @InjectMocks private FeeCalculationService feeCalculationService;

  @Nested
  @DisplayName("validateFeeCalculation")
  class ValidateFeeCalculationTests {

    @Test
    @DisplayName("Successful validation does not update context")
    void successfulValidationDoesNotUpdateContext() {

      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");

      FeeCalculationRequest feeCalculationRequest = new FeeCalculationRequest().feeCode("feeCode");
      FeeCalculationResponse feeCalculationResponse = new FeeCalculationResponse();

      when(feeSchemeMapper.mapToFeeCalculationRequest(claim)).thenReturn(feeCalculationRequest);
      when(feeSchemePlatformRestClient.calculateFee(feeCalculationRequest))
          .thenReturn(ResponseEntity.ok(feeCalculationResponse));
      when(validationContext.isFlaggedForRetry("claimId")).thenReturn(false);

      feeCalculationService.validateFeeCalculation(claim);

      verify(feeSchemePlatformRestClient, times(1)).calculateFee(feeCalculationRequest);
    }

    @Test
    @DisplayName("Warning in fee calculation response results in claim error added to context")
    void warningResponseResultsInClaimErrorAddedToContext() {

      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");

      FeeCalculationRequest feeCalculationRequest = new FeeCalculationRequest().feeCode("feeCode");

      Warning warning = new Warning();
      warning.setWarningDescription("warningDescription");
      FeeCalculationResponse feeCalculationResponse = new FeeCalculationResponse().warning(warning);

      when(feeSchemeMapper.mapToFeeCalculationRequest(claim)).thenReturn(feeCalculationRequest);
      when(feeSchemePlatformRestClient.calculateFee(feeCalculationRequest))
          .thenReturn(ResponseEntity.ok(feeCalculationResponse));
      when(validationContext.isFlaggedForRetry("claimId")).thenReturn(false);

      feeCalculationService.validateFeeCalculation(claim);

      verify(feeSchemePlatformRestClient, times(1)).calculateFee(feeCalculationRequest);
      verify(validationContext, times(1))
          .addClaimError("claimId", ClaimValidationError.INVALID_FEE_CALCULATION_VALIDATION_FAILED);
    }

    @Test
    @DisplayName("404 Not found response results in claim being flagged for retry")
    void notFoundResponseResultsInClaimBeingFlaggedForRetry() {

      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");

      FeeCalculationRequest feeCalculationRequest = new FeeCalculationRequest().feeCode("feeCode");

      when(feeSchemeMapper.mapToFeeCalculationRequest(claim)).thenReturn(feeCalculationRequest);
      when(feeSchemePlatformRestClient.calculateFee(feeCalculationRequest))
          .thenReturn(ResponseEntity.notFound().build());
      when(validationContext.isFlaggedForRetry("claimId")).thenReturn(false);

      ThrowingCallable result = () -> feeCalculationService.validateFeeCalculation(claim);

      feeCalculationService.validateFeeCalculation(claim);

      verify(feeSchemePlatformRestClient, times(1)).calculateFee(feeCalculationRequest);

      verify(validationContext, times(1)).flagForRetry(claim.getId());
    }

    @Test
    @DisplayName("500 Server error response results in claim being flagged for retry")
    void serverErrorResponseResultsInClaimBeingFlaggedForRetry() {

      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");

      FeeCalculationRequest feeCalculationRequest = new FeeCalculationRequest().feeCode("feeCode");

      when(feeSchemeMapper.mapToFeeCalculationRequest(claim)).thenReturn(feeCalculationRequest);
      when(feeSchemePlatformRestClient.calculateFee(feeCalculationRequest))
          .thenReturn(ResponseEntity.internalServerError().build());
      when(validationContext.isFlaggedForRetry("claimId")).thenReturn(false);

      feeCalculationService.validateFeeCalculation(claim);

      verify(feeSchemePlatformRestClient, times(1)).calculateFee(feeCalculationRequest);

      verify(validationContext, times(1)).flagForRetry(claim.getId());
    }

    @Test
    @DisplayName("Skips validation if claim is flagged for retry")
    void skipsValidationIfClaimIsFlaggedForRetry() {

      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");

      when(validationContext.isFlaggedForRetry("claimId")).thenReturn(true);

      feeCalculationService.validateFeeCalculation(claim);

      verifyNoInteractions(feeSchemeMapper);
      verifyNoInteractions(feeSchemePlatformRestClient);
    }
  }
}
