package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.FEE_SERVICE;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.FeeSchemeMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationRequest;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;
import uk.gov.justice.laa.fee.scheme.model.ValidationMessagesInner;

/**
 * Service responsible for validating the fee calculation response from the Fee Scheme Platform API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeCalculationService {

  private final FeeSchemePlatformRestClient feeSchemePlatformRestClient;
  private final FeeSchemeMapper feeSchemeMapper;
  private final FeeCalculationUpdaterService feeCalculationUpdaterService;

  /**
   * Calculates the fee for the claim using the Fee Scheme Platform API, and handles any returned
   * validation errors.
   *
   * @param submissionId the ID of the submission to validate
   * @param claim the submitted claim
   * @param context the validation context to add errors to
   * @param feeDetailsResponse the fee details response for the feeCode associate with the claim
   */
  public void validateFeeCalculation(
      UUID submissionId,
      ClaimResponse claim,
      SubmissionValidationContext context,
      FeeDetailsResponse feeDetailsResponse) {
    log.debug("Validating fee calculation for claim {}", claim.getId());
    if (StringUtils.hasText(claim.getFeeCode()) && !context.isFlaggedForRetry(claim.getId())) {
      FeeCalculationRequest feeCalculationRequest =
          feeSchemeMapper.mapToFeeCalculationRequest(claim);

      try {
        ResponseEntity<FeeCalculationResponse> response =
            feeSchemePlatformRestClient.calculateFee(feeCalculationRequest);

        FeeCalculationResponse feeCalculationResponse = response.getBody();
        if (feeCalculationResponse == null) {
          log.debug("Fee calculation returned an empty response");
          context.flagForRetry(claim.getId());
          return;
        }

        // Patch claim with fee calculation response
        feeCalculationUpdaterService.updateClaimWithFeeCalculationDetails(
            submissionId, claim, feeCalculationResponse, feeDetailsResponse);

        List<ValidationMessagesInner> validationMessages =
            feeCalculationResponse.getValidationMessages();

        if (validationMessages != null && !validationMessages.isEmpty()) {
          for (var m : validationMessages) {
            if (ValidationMessagesInner.TypeEnum.ERROR.equals(m.getType())) {
              log.debug("Fee calculation returned validation error: {}", m);
              context.addClaimError(claim.getId(), m.getMessage(), FEE_SERVICE);
            } else if (ValidationMessagesInner.TypeEnum.WARNING.equals(m.getType())) {
              log.debug("Fee calculation returned validation warning: {}", m);
              context.addClaimWarning(claim.getId(), m.getMessage(), FEE_SERVICE);
            }
          }
        }
      } catch (WebClientResponseException ex) {
        handleWebClientError(ex, claim, context);
      } catch (Exception ex) {
        handleUnexpectedError(ex, claim, context);
      }
    }
    log.debug("Fee calculation validation completed for claim {}", claim.getId());
  }

  private void handleUnexpectedError(
      Exception ex, ClaimResponse claim, SubmissionValidationContext context) {
    log.error("Unexpected error during fee calculation", ex);
    context.addClaimMessages(
        claim.getId(),
        getValidationMessagePatches(
            ex, ClaimValidationError.TECHNICAL_ERROR_FEE_CALCULATION_SERVICE));
  }

  private void handleWebClientError(
      WebClientResponseException ex, ClaimResponse claim, SubmissionValidationContext context) {
    ClaimValidationError errorType =
        (ex instanceof WebClientResponseException.BadRequest)
            ? ClaimValidationError.INVALID_FEE_CALCULATION_VALIDATION_FAILED
            : ClaimValidationError.TECHNICAL_ERROR_FEE_CALCULATION_SERVICE;

    log.error(
        "Fee calculation request failed with status {}: {}",
        ex.getStatusCode(),
        ex.getResponseBodyAsString(),
        ex);

    context.addClaimMessages(claim.getId(), getValidationMessagePatches(ex, errorType));
  }

  private static List<ValidationMessagePatch> getValidationMessagePatches(
      Exception ex, ClaimValidationError claimValidationError) {
    return List.of(
        new ValidationMessagePatch()
            .displayMessage(claimValidationError.getDisplayMessage())
            .technicalMessage(ex.getMessage())
            .source(claimValidationError.getSource())
            .type(claimValidationError.getType()));
  }
}
