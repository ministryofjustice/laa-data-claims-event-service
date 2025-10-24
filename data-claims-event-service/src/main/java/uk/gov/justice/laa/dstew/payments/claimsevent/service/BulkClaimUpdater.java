package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

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

  /**
   * Update all claims in a submission via the Data Claims API, depending on the result of their
   * validation.
   *
   * <ul>
   *   <li>If validation errors have been recorded, update the claim status to INVALID and send
   *       through the errors.
   *   <li>If no errors have been recorded, update the claim status to VALID
   *   <li>If the context has errors, or the submission has a previous claim which is INVALID, then
   *       mark all claims as INVALID.
   * </ul>
   *
   * @param submission the claim submission
   * @param context the submission validation context
   */
  public void updateClaims(SubmissionResponse submission, SubmissionValidationContext context) {
    log.debug("Updating claims for submission {}", submission.getSubmissionId().toString());
    AtomicInteger claimsUpdated = new AtomicInteger();
    AtomicInteger claimsFlaggedForRetry = new AtomicInteger();

    // Flag any claims that have been flagged for retry
    List<SubmissionClaim> claims =
        Optional.ofNullable(submission.getClaims()).orElse(Collections.emptyList());

    // Check if any claims are invalid
    boolean invalidClaimExists =
        claims.stream().anyMatch(claim -> ClaimStatus.INVALID.equals(claim.getStatus()))
            || context.hasClaimLevelErrors();

    claims.stream()
        .filter(claim -> ClaimStatus.READY_TO_PROCESS.equals(claim.getStatus()))
        .forEach(
            claim -> {
              // Get claim ID
              String claimId = claim.getClaimId().toString();

              // Skip claims flagger for retry
              if (context.isFlaggedForRetry(claimId)) {
                log.debug("Claim {} is flagged for retry. Skipping update.", claim.getClaimId());
                claimsFlaggedForRetry.incrementAndGet();
                return;
              }

              // If a claim was found to be invalid, make the rest of the claims invalid
              ClaimStatus claimStatus =
                  invalidClaimExists ? ClaimStatus.INVALID : ClaimStatus.VALID;
              List<ValidationMessagePatch> claimMessages = getClaimMessages(claimId, context);

              ClaimPatch claimPatch =
                  ClaimPatch.builder()
                      .id(claimId)
                      .status(claimStatus)
                      .validationMessages(claimMessages)
                      .build();
              dataClaimsRestClient.updateClaim(
                  submission.getSubmissionId(), claim.getClaimId(), claimPatch);
              log.debug("Claim {} status updated to {}", claimId, claimStatus);
              claimsUpdated.getAndIncrement();
            });
    log.debug(
        "Claim updates completed for submission {}. Claims updated: {}. "
            + "Claim updates skipped: {}",
        submission.getSubmissionId(),
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
}
