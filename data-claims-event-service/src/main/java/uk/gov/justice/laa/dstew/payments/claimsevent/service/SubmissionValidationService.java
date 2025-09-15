package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.SubmissionValidationException;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleLine;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

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
  private final ProviderDetailsRestClient providerDetailsRestClient;

  /**
   * Validates a claim submission.
   *
   * @param submissionId the ID of the submission to validate
   */
  public void validateSubmission(UUID submissionId) {
    SubmissionResponse submission =
        dataClaimsRestClient.getSubmission(submissionId.toString()).getBody();

    log.debug("Validating submission {}", submissionId);

    verifySubmissionStatus(submission);

    SubmissionValidationContext context = initialiseValidationContext(submission);

    validateNilSubmission(submission, context);

    String officeCode = submission.getOfficeAccountNumber();
    String areaOfLaw = submission.getAreaOfLaw();
    List<String> providerCategoriesOfLaw = getProviderCategoriesOfLaw(officeCode, areaOfLaw);
    validateProviderContract(submissionId.toString(), providerCategoriesOfLaw, context);

    claimValidationService.validateClaims(submission, providerCategoriesOfLaw, context);

    // TODO: Send through all claim errors in the patch request.
    updateClaims(submission, context);

    // TODO: Verify all claims have been validated, and update submission status to
    //  VALIDATION_SUCCEEDED or VALIDATION_FAILED
    //  If unvalidated claims remain, re-queue message.
    log.debug("Validation completed for submission {}", submissionId);
  }

  private void verifySubmissionStatus(SubmissionResponse submission) {
    SubmissionStatus currentStatus = submission.getStatus();
    UUID submissionId = submission.getSubmissionId();
    switch (currentStatus) {
      case VALIDATION_IN_PROGRESS ->
          log.debug(
              "Submission {} already under validation. Attempting to complete validation.",
              submissionId);
      case READY_FOR_VALIDATION -> {
        log.debug(
            "Submission {} ready for validation. Updating status to VALIDATION_IN_PROGRESS.",
            submissionId);
        updateSubmissionStatus(submissionId, SubmissionStatus.VALIDATION_IN_PROGRESS);
      }
      case null -> {
        log.debug("Submission {} state is null", submissionId);
        throw new SubmissionValidationException("Submission state is null");
      }
      default -> {
        log.debug(
            "Submission {} cannot be validated in its current state: {}",
            submissionId,
            currentStatus);
        throw new SubmissionValidationException(
            "Submission cannot be validated in state " + currentStatus);
      }
    }
  }

  private void updateSubmissionStatus(UUID submissionId, SubmissionStatus submissionStatus) {
    SubmissionPatch submissionPatch =
        new SubmissionPatch().submissionId(submissionId).status(submissionStatus);
    dataClaimsRestClient.updateSubmission(submissionId.toString(), submissionPatch);
  }

  private void validateNilSubmission(
      SubmissionResponse submission, SubmissionValidationContext context) {
    log.debug("Validating nil submission for submission {}", submission.getSubmissionId());
    if (Boolean.TRUE.equals(submission.getIsNilSubmission())) {
      if (submission.getClaims() != null && !submission.getClaims().isEmpty()) {
        context.addToAllClaimReports(ClaimValidationError.INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS);
      }
    } else if (Boolean.FALSE.equals(submission.getIsNilSubmission())) {
      if (submission.getClaims() == null || submission.getClaims().isEmpty()) {
        throw new SubmissionValidationException(
            "Submission is not marked as nil submission, but does not contain any claims");
      }
    }
    log.debug("Nil submission completed for submission {}", submission.getSubmissionId());
  }

  private List<String> getProviderCategoriesOfLaw(String officeCode, String areaOfLaw) {
    return providerDetailsRestClient
        .getProviderFirmSchedules(officeCode, areaOfLaw)
        .blockOptional()
        .stream()
        .map(ProviderFirmOfficeContractAndScheduleDto::getSchedules)
        .flatMap(List::stream)
        .map(FirmOfficeContractAndScheduleDetails::getScheduleLines)
        .flatMap(List::stream)
        .map(FirmOfficeContractAndScheduleLine::getCategoryOfLaw)
        .toList();
  }

  private void validateProviderContract(
      String submissionId,
      List<String> providerCategoriesOfLaw,
      SubmissionValidationContext context) {
    log.debug("Validating provider contract for submission {}", submissionId);
    if (providerCategoriesOfLaw.isEmpty()) {
      context.addToAllClaimReports(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);
    }
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
   */
  private void updateClaims(SubmissionResponse submission, SubmissionValidationContext context) {
    log.debug("Updating claims for submission {}", submission.getSubmissionId().toString());
    AtomicInteger claimsUpdated = new AtomicInteger();
    AtomicInteger claimsFlaggedForRetry = new AtomicInteger();
    submission.getClaims().stream()
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
              List<String> claimErrors = getClaimErrors(claimId, context);
              ClaimPatch claimPatch =
                  ClaimPatch.builder()
                      .id(claimId)
                      .status(claimStatus)
                      .validationErrors(claimErrors)
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

  private List<String> getClaimErrors(String claimId, SubmissionValidationContext context) {
    return context.getClaimReport(claimId).stream()
        .map(ClaimValidationReport::getErrors)
        .flatMap(List::stream)
        .map(ClaimValidationError::getDescription)
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
