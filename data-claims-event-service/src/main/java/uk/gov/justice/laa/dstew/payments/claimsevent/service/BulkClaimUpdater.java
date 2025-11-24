package uk.gov.justice.laa.dstew.payments.claimsevent.service;

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
   * Calculates the fee for the claim using the Fee Scheme Platform API, and handles any returned
   * errors.
   *
   * <ul>
   *   <li>If validation errors have been recorded, update the claim status to INVALID and send
   *       through the errors.
   *   <li>If no errors have been recorded, update the claim status to VALID.
   *   <li>If the context has a claim errors, then mark the claim as INVALID.
   * </ul>
   *
   * @param submissionId the submission ID
   * @param claimResponses the list of claim responses
   * @param context the submission validation context
   */
  public void updateClaims(
      UUID submissionId,
      List<ClaimResponse> claimResponses,
      AreaOfLaw areaOfLaw,
      SubmissionValidationContext context,
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap) {
    log.debug("Updating claims for submission {}", submissionId);
    AtomicInteger claimsUpdated = new AtomicInteger();
    AtomicInteger claimsFlaggedForRetry = new AtomicInteger();

    claimResponses.forEach(
        claim -> {
          // Get claim ID
          String claimId = claim.getId();

          // Skip claims flagged for retry
          if (context.isFlaggedForRetry(claimId) && !context.hasErrors(claimId)) {
            log.debug("Claim {} is flagged for retry. Skipping update.", claimId);
            claimsFlaggedForRetry.incrementAndGet();
            return;
          }

          eventServiceMetricService.startFspValidationTimer(UUID.fromString(claim.getId()));

          Optional<FeeCalculationResponse> feeCalculationResponse =
              feeCalculationService.calculateFee(claim, context, areaOfLaw);

          eventServiceMetricService.stopFspValidationTimer(UUID.fromString(claim.getId()));
          FeeDetailsResponseWrapper feeDetailsResponseWrapper =
              feeDetailsResponseMap.get(claim.getFeeCode());

          FeeCalculationPatch feeCalculationPatch =
              feeCalculationPatchMapper.mapToFeeCalculationPatch(
                  feeCalculationResponse.orElse(null),
                  feeDetailsResponseWrapper.getFeeDetailsResponse());

          // If a claim was found to be invalid, make the rest of the claims invalid
          ClaimStatus claimStatus = getClaimStatus(claimId, context);
          List<ValidationMessagePatch> claimMessages = getClaimMessages(claimId, context);

          ClaimPatch claimPatch =
              ClaimPatch.builder()
                  .id(claimId)
                  .status(claimStatus)
                  .feeCalculationResponse(feeCalculationPatch)
                  .validationMessages(claimMessages)
                  .build();
          dataClaimsRestClient.updateClaim(submissionId, UUID.fromString(claimId), claimPatch);
          log.debug("Claim {} status updated to {}", claimId, claimStatus);
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
      String claimId, SubmissionValidationContext context) {
    return context.getClaimReport(claimId).stream()
        .map(ClaimValidationReport::getMessages)
        .flatMap(List::stream)
        .toList();
  }

  private ClaimStatus getClaimStatus(String claimId, SubmissionValidationContext context) {
    if (context.hasSubmissionLevelErrors() || context.hasErrors(claimId)) {
      return ClaimStatus.INVALID;
    } else {
      return ClaimStatus.VALID;
    }
  }
}
