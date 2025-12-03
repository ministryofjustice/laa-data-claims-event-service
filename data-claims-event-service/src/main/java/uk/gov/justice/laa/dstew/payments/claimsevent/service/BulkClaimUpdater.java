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
                    feeCalculationResponse.get(), feeDetailsResponseMap.get(claim.getFeeCode()));
          }

          // If a claim was found to be invalid, make the rest of the claims invalid
          ClaimStatus claimStatus = getClaimStatus(claim.getId(), context);
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
      final String claimId, final SubmissionValidationContext context) {
    return (context.hasSubmissionLevelErrors() || context.hasErrors(claimId))
        ? ClaimStatus.INVALID
        : ClaimStatus.VALID;
  }

  private Optional<FeeCalculationResponse> getFeeCalculationResponse(
      final AreaOfLaw areaOfLaw,
      final SubmissionValidationContext context,
      final ClaimResponse claim) {

    Optional<FeeCalculationResponse> feeCalculationResponse =
        feeCalculationService.calculateFee(claim, context, areaOfLaw);

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
      final FeeDetailsResponseWrapper feeDetailsResponseWrapper) {
    return feeCalculationPatchMapper.mapToFeeCalculationPatch(
        feeCalculationResponse, feeDetailsResponseWrapper.getFeeDetailsResponse());
  }
}
