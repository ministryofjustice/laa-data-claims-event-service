package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.FeeCalculationPatchMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;

/**
 * A service responsible for updating claim statuses in the Data Claims API.
 *
 * @author Jamie Briggs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkClaimUpdater {

  private final DataClaimsRestClient dataClaimsRestClient;
  private final FeeCalculationService feeCalculationService;
  private final EventServiceMetricService eventServiceMetricService;
  private final FeeCalculationPatchMapper feeCalculationPatchMapper;

  /**
   * Calculates fees and updates claims after initial validation. Claims without validation errors
   * are held in {@code VALIDATED_PENDING_APPROVAL}. Claims with submission-level or their own
   * validation errors become {@code INVALID}. Claims already flagged for retry without errors are
   * skipped.
   *
   * @param submissionId the submission ID
   * @param claimResponses the list of claim responses
   * @param areaOfLaw the area of law
   * @param context the submission validation context
   * @param feeDetailsResponseMap the fee details response map
   */
  public void updateClaims(
      UUID submissionId,
      List<ClaimResponse> claimResponses,
      AreaOfLaw areaOfLaw,
      SubmissionValidationContext context,
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap) {
    updateClaims(submissionId, claimResponses, areaOfLaw, context, feeDetailsResponseMap, false);
  }

  /**
   * Updates claims with the result of either initial or final validation.
   *
   * <p>Claims without validation errors become {@code VALIDATED_PENDING_APPROVAL} after initial
   * validation or {@code VALID} after final validation. Submission-level or claim-specific errors
   * result in {@code INVALID}. Claims already flagged for retry without errors are skipped.
   *
   * @param submissionId the submission ID
   * @param claimResponses the claims being updated
   * @param areaOfLaw the submission area of law
   * @param context validation context
   * @param feeDetailsResponseMap fee details used by fee calculation
   * @param finalValidation whether the claims are being accepted after final validation
   */
  public void updateClaims(
      UUID submissionId,
      List<ClaimResponse> claimResponses,
      AreaOfLaw areaOfLaw,
      SubmissionValidationContext context,
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap,
      boolean finalValidation) {
    log.debug("Updating claims for submission {}", submissionId);
    AtomicInteger claimsUpdated = new AtomicInteger();
    AtomicInteger claimsFlaggedForRetry = new AtomicInteger();

    claimResponses.forEach(
        claim -> {
          if (context.isFlaggedForRetry(claim.getId()) && !context.hasErrors(claim.getId())) {
            log.debug("Claim {} is flagged for retry. Skipping update.", claim.getId());
            claimsFlaggedForRetry.incrementAndGet();
            return;
          }

          Optional<FeeCalculationResponse> feeCalculationResponse =
              getFeeCalculationResponse(areaOfLaw, context, claim);

          FeeCalculationPatch feeCalculationPatch = null;

          if (feeCalculationResponse.isPresent()) {
            feeCalculationPatch =
                buildFeeCalculationPatch(
                    feeCalculationResponse.get(),
                    feeDetailsResponseMap.get(claim.getFeeCode()),
                    context.getAuthorisedCategoryOfLawCode(claim.getId()));
          }

          ClaimStatus claimStatus = getClaimStatus(claim.getId(), context, finalValidation);
          ClaimPatch claimPatch = buildClaimPatch(claim, feeCalculationPatch, context, claimStatus);

          // Update claim regardless of fee calculation presence
          dataClaimsRestClient.updateClaim(
              submissionId, UUID.fromString(claim.getId()), claimPatch);

          log.debug("Claim {} status updated to {}", claim.getId(), claimStatus);
          claimsUpdated.getAndIncrement();
        });
    log.debug(
        "Claim updates completed for submission {}. Claims updated: {}. "
            + "Claim updates skipped: {}",
        submissionId,
        claimsUpdated.get(),
        claimsFlaggedForRetry.get());
  }

  private List<ValidationMessagePatch> getClaimMessages(
      final String claimId, final SubmissionValidationContext context) {
    return context.getClaimReport(claimId).stream()
        .map(ClaimValidationReport::getMessages)
        .flatMap(List::stream)
        .toList();
  }

  private ClaimStatus getClaimStatus(
      final String claimId,
      final SubmissionValidationContext context,
      final boolean finalValidation) {
    if (context.hasSubmissionLevelErrors() || context.hasErrors(claimId)) {
      return ClaimStatus.INVALID;
    }
    return finalValidation ? ClaimStatus.VALID : ClaimStatus.VALIDATED_PENDING_APPROVAL;
  }

  private Optional<FeeCalculationResponse> getFeeCalculationResponse(
      final AreaOfLaw areaOfLaw,
      final SubmissionValidationContext context,
      final ClaimResponse claim) {
    eventServiceMetricService.startFspValidationTimer(UUID.fromString(claim.getId()));

    Optional<FeeCalculationResponse> feeCalculationResponse =
        feeCalculationService.calculateFee(claim, context, areaOfLaw);

    eventServiceMetricService.stopFspValidationTimer(UUID.fromString(claim.getId()));
    return feeCalculationResponse;
  }

  private ClaimPatch buildClaimPatch(
      final ClaimResponse claim,
      final FeeCalculationPatch feeCalculationPatch,
      final SubmissionValidationContext context,
      final ClaimStatus claimStatus) {

    List<ValidationMessagePatch> claimMessages = getClaimMessages(claim.getId(), context);
    return ClaimPatch.builder()
        .id(claim.getId())
        .status(claimStatus)
        .feeCalculationResponse(feeCalculationPatch)
        .validationMessages(claimMessages)
        .createdByUserId(EVENT_SERVICE)
        .build();
  }

  private FeeCalculationPatch buildFeeCalculationPatch(
      final FeeCalculationResponse feeCalculationResponse,
      final FeeDetailsResponseWrapper feeDetailsResponseWrapper,
      final String categoryOfLawCode) {
    return feeCalculationPatchMapper.mapToFeeCalculationPatch(
        feeCalculationResponse,
        feeDetailsResponseWrapper.getFeeDetailsResponse(),
        categoryOfLawCode);
  }
}
