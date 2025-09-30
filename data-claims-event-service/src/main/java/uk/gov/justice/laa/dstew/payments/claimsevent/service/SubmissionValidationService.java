package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
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
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission.SubmissionValidator;

/**
 * A service responsible for validating claim submissions. Any errors found during validation will
 * be reported against the appropriate claims. Once validation is complete, the claim status and any
 * validation errors will be sent to the Data Claims API.
 */
@Slf4j
@Service
@AllArgsConstructor
public class SubmissionValidationService {

  private final ClaimValidationService claimValidationService;
  private final DataClaimsRestClient dataClaimsRestClient;
  private final List<SubmissionValidator> submissionValidatorList;

  /**
   * Validates a claim submission inside the provided submissionResponse.
   *
   * @param submissionId the ID of the submission to validate
   */
  public SubmissionValidationContext validateSubmission(UUID submissionId) {
    SubmissionResponse submission = dataClaimsRestClient.getSubmission(submissionId).getBody();

    log.debug("Validating submission {}", submissionId);

    SubmissionValidationContext context = initialiseValidationContext(submission);

    // Currently validating:
    // - Submission Status (Has highest priority to update the submission status if required)
    // - Submission Schema
    // - Nil submissions
    submissionValidatorList.stream()
        .sorted(Comparator.comparingInt(SubmissionValidator::priority))
        .forEach(validator -> validator.validate(submission, context));

    // Only validate claims if no validation errors have been recorded.
    if (!context.hasErrors()) {
      claimValidationService.validateClaims(submission, context);

      // TODO: Send through all claim errors in the patch request.
      // TODO: Verify all claims have been validated, and update submission status to
      updateClaims(submission, context);
    }
    //  VALIDATION_SUCCEEDED or VALIDATION_FAILED
    //  If unvalidated claims remain, re-queue message.
    log.debug("Validation completed for submission {}", submissionId);

    return context;
  }

  /**
   * Update all claims in a submission via the Data Claims API, depending on the result of their
   * validation.
   *
   * <ul>
   *   <li>If validation errors have been recorded, update the claim status to INVALID and send
   *       through the errors.
   *   <li>If no errors have been recorded, update the claim status to VALID
   *   <li>If the claim has been flagged for retry due to an unexpected error during validation,
   *       skip the update for this claim.
   * </ul>
   *
   * @param submission the claim submission
   * @param context the submission validation context
   */
  private void updateClaims(SubmissionResponse submission, SubmissionValidationContext context) {
    log.debug("Updating claims for submission {}", submission.getSubmissionId().toString());
    AtomicInteger claimsUpdated = new AtomicInteger();
    AtomicInteger claimsFlaggedForRetry = new AtomicInteger();
    // Get submission claims in a null safe way
    Optional.ofNullable(submission.getClaims()).stream()
        .flatMap(List::stream)
        .filter(claim -> ClaimStatus.READY_TO_PROCESS.equals(claim.getStatus()))
        .peek(
            claim -> {
              if (context.isFlaggedForRetry(claim.getClaimId().toString())) {
                log.debug("Claim {} is flagged for retry. Skipping update.", claim.getClaimId());
                claimsFlaggedForRetry.incrementAndGet();
              }
            })
        .filter(claim -> !context.isFlaggedForRetry(claim.getClaimId().toString()))
        .forEach(
            claim -> {
              String claimId = claim.getClaimId().toString();
              ClaimStatus claimStatus = getClaimStatus(claimId, context);
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

  private ClaimStatus getClaimStatus(String claimId, SubmissionValidationContext context) {
    if (context.hasErrors(claimId)) {
      return ClaimStatus.INVALID;
    } else {
      return ClaimStatus.VALID;
    }
  }

  private List<ValidationMessagePatch> getClaimMessages(
      String claimId, SubmissionValidationContext context) {
    return context.getClaimReport(claimId).stream()
        .map(ClaimValidationReport::getMessages)
        .flatMap(List::stream)
        .toList();
  }

  private SubmissionValidationContext initialiseValidationContext(SubmissionResponse submission) {
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    if (submission.getClaims() == null) {
      return submissionValidationContext;
    }
    List<ClaimValidationReport> claimReports =
        submission.getClaims().stream()
            .filter(claim -> ClaimStatus.READY_TO_PROCESS.equals(claim.getStatus()))
            .map(SubmissionClaim::getClaimId)
            .map(UUID::toString)
            .map(ClaimValidationReport::new)
            .toList();
    submissionValidationContext.addClaimReports(claimReports);
    return submissionValidationContext;
  }
}
