package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.FEE_SERVICE;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.FeeSchemeMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationRequest;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;
import uk.gov.justice.laa.fee.scheme.model.ValidationMessagesInner;
import uk.gov.laa.springboot.metrics.aspect.annotations.SummaryTimerMetric;

/**
 * Service responsible for validating the fee calculation response from the Fee Scheme Platform API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeCalculationService {

  private final FeeSchemePlatformRestClient feeSchemePlatformRestClient;
  private final FeeSchemeMapper feeSchemeMapper;

  /**
   * Calculates the fee for the claim using the Fee Scheme Platform API, and handles any returned
   * validation errors.
   *
   * @param claim the submitted claim
   * @param context the validation context to add errors to
   */
  @SummaryTimerMetric(
      metricName = "fsp_validation_time",
      hintText = "Total time taken to validate fee calculation using FSP API")
  public Optional<FeeCalculationResponse> calculateFee(
      ClaimResponse claim, SubmissionValidationContext context, AreaOfLaw areaOfLaw) {
    log.debug("Validating fee calculation for claim {}", claim.getId());
    FeeCalculationResponse feeCalculationResponse = null;
    if (StringUtils.hasText(claim.getFeeCode()) && !context.isFlaggedForRetry(claim.getId())) {
      FeeCalculationRequest feeCalculationRequest =
          feeSchemeMapper.mapToFeeCalculationRequest(claim, areaOfLaw);
      log.debug("Fee calculation request: {}", feeCalculationRequest);
      try {
        ResponseEntity<FeeCalculationResponse> response =
            feeSchemePlatformRestClient.calculateFee(feeCalculationRequest);

        feeCalculationResponse = response.getBody();

        if (feeCalculationResponse == null) {
          log.debug("Fee calculation returned an empty response");
          context.flagForRetry(claim.getId());
          return Optional.empty();
        }
        log.debug("Fee calculation response: {}", feeCalculationResponse);

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
      }
    }
    log.debug("Fee calculation validation completed for claim {}", claim.getId());
    return Optional.ofNullable(feeCalculationResponse);
  }

  private void handleWebClientError(
      WebClientResponseException ex, ClaimResponse claim, SubmissionValidationContext context) {
    ClaimValidationError errorType = getClaimValidationError(ex);

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

  private static ClaimValidationError getClaimValidationError(WebClientResponseException ex) {
    return switch (ex.getStatusCode().value()) {
      case 400, 404 -> ClaimValidationError.INVALID_FEE_CALCULATION_VALIDATION_FAILED;
      default -> ClaimValidationError.TECHNICAL_ERROR_FEE_CALCULATION_SERVICE;
    };
  }
}
