package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.SubmissionValidationException;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.FeeSchemeMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationRequest;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;
import uk.gov.justice.laa.fee.scheme.model.Warning;

/**
 * Service responsible for validating the fee calculation response from the Fee Scheme Platform API.
 */
@Service
@RequiredArgsConstructor
public class FeeCalculationService {

  private final SubmissionValidationContext validationContext;
  private final FeeSchemePlatformRestClient feeSchemePlatformRestClient;
  private final FeeSchemeMapper feeSchemeMapper;

  /**
   * Calculates the fee for the claim using the Fee Scheme Platform API, and handles any returned
   * validation errors.
   *
   * @param claim the submitted claim
   */
  public void validateFeeCalculation(ClaimFields claim) {
    FeeCalculationRequest feeCalculationRequest = feeSchemeMapper.mapToFeeCalculationRequest(claim);

    ResponseEntity<FeeCalculationResponse> response =
        feeSchemePlatformRestClient.calculateFee(feeCalculationRequest);

    if (response.getStatusCode().is2xxSuccessful()) {
      FeeCalculationResponse feeCalculationResponse = response.getBody();
      if (feeCalculationResponse == null) {
        throw new SubmissionValidationException("Fee calculation returned null response");
      }
      // TODO: Get all individual errors from Fee Scheme Platform API?
      Warning warning = feeCalculationResponse.getWarning();
      if (warning != null && warning.getWarningDescription() != null) {
        validationContext.addClaimError(
            claim.getId(), ClaimValidationError.INVALID_FEE_CALCULATION_VALIDATION_FAILED);
      }
    } else {
      throw new SubmissionValidationException(
          "Fee calculation returned unsuccessful response with status: "
              + response.getStatusCode());
    }
  }
}
