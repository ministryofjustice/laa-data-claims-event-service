package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
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
  private final SubmissionValidationContext submissionValidationContext;
  private final JsonSchemaValidator jsonSchemaValidator;

  /**
   * Validates a claim submission inside the provided submissionResponse.
   *
   * @param submission the submission to validate
   */
  public void validateSubmission(SubmissionResponse submission) {
    UUID submissionId = submission.getSubmissionId();

    log.debug("Validating submission {}", submissionId);

    verifySubmissionStatus(submissionId, submission.getStatus());

    submissionValidationContext.addSubmissionValidationErrors(
        jsonSchemaValidator.validate("submission", submission));

    List<ClaimResponse> claims = getReadyToProcessClaims(submission);

    initialiseValidationContext(claims);

    validateNilSubmission(submission);

    String officeCode = submission.getOfficeAccountNumber();
    String areaOfLaw = submission.getAreaOfLaw();
    List<String> providerCategoriesOfLaw = getProviderCategoriesOfLaw(officeCode, areaOfLaw);
    validateProviderContract(submissionId.toString(), providerCategoriesOfLaw);

    claimValidationService.validateClaims(claims, providerCategoriesOfLaw, areaOfLaw);

    // TODO: Send through all claim errors in the patch request.
    updateClaims(submissionId, claims);

    // TODO: Verify all claims have been validated, and update submission status to
    //  VALIDATION_SUCCEEDED or VALIDATION_FAILED
    //  If unvalidated claims remain, re-queue message.
    log.debug("Validation completed for submission {}", submissionId);
  }

  private void verifySubmissionStatus(UUID submissionId, SubmissionStatus currentStatus) {
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
        submissionValidationContext.addSubmissionValidationError("Submission state is null");
      }
      default -> {
        log.debug(
            "Submission {} cannot be validated in its current state: {}",
            submissionId,
            currentStatus);
        submissionValidationContext.addSubmissionValidationError(
            "Submission cannot be validated in state " + currentStatus);
      }
    }
  }

  private void updateSubmissionStatus(UUID submissionId, SubmissionStatus submissionStatus) {
    SubmissionPatch submissionPatch =
        new SubmissionPatch().submissionId(submissionId).status(submissionStatus);
    dataClaimsRestClient.updateSubmission(submissionId.toString(), submissionPatch);
  }

  private void validateNilSubmission(SubmissionResponse submission) {
    log.debug("Validating nil submission for submission {}", submission.getSubmissionId());
    if (Boolean.TRUE.equals(submission.getIsNilSubmission())) {
      if (submission.getClaims() != null && !submission.getClaims().isEmpty()) {
        submissionValidationContext.addSubmissionValidationError(
            ClaimValidationError.INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS.getDescription());
      }
    } else if (Boolean.FALSE.equals(submission.getIsNilSubmission())
        && (submission.getClaims() == null || submission.getClaims().isEmpty())) {
      submissionValidationContext.addSubmissionValidationError(
          ClaimValidationError.NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS.getDescription());
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

  private void validateProviderContract(String submissionId, List<String> providerCategoriesOfLaw) {
    log.debug("Validating provider contract for submission {}", submissionId);
    if (providerCategoriesOfLaw.isEmpty()) {
      submissionValidationContext.addSubmissionValidationError(
          ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER.getDescription());
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
   * @param submissionId the ID of the submission
   * @param claims the claims in the submission
   */
  private void updateClaims(UUID submissionId, List<ClaimResponse> claims) {
    log.debug("Updating claims for submission {}", submissionId.toString());
    AtomicInteger claimsUpdated = new AtomicInteger();
    AtomicInteger claimsFlaggedForRetry = new AtomicInteger();
    claims.stream()
        .peek(
            claim -> {
              if (submissionValidationContext.isFlaggedForRetry(claim.getId())) {
                log.debug("Claim {} is flagged for retry. Skipping update.", claim.getId());
                claimsFlaggedForRetry.incrementAndGet();
              }
            })
        .filter(claim -> !submissionValidationContext.isFlaggedForRetry(claim.getId()))
        .forEach(
            claim -> {
              ClaimStatus claimStatus = getClaimStatus(claim.getId());
              List<String> claimErrors = getClaimErrors(claim.getId());
              ClaimPatch claimPatch =
                  ClaimPatch.builder()
                      .id(claim.getId())
                      .status(claimStatus)
                      .validationErrors(claimErrors)
                      .build();
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

  private ClaimStatus getClaimStatus(String claimId) {
    if (submissionValidationContext.hasErrors(claimId)) {
      return ClaimStatus.INVALID;
    } else {
      return ClaimStatus.VALID;
    }
  }

  private List<String> getClaimErrors(String claimId) {
    return submissionValidationContext.getClaimReport(claimId).stream()
        .map(ClaimValidationReport::getErrors)
        .flatMap(List::stream)
        .toList();
  }

  private void initialiseValidationContext(List<ClaimResponse> claims) {
    List<ClaimValidationReport> claimValidationErrors =
        claims.stream().map(ClaimResponse::getId).map(ClaimValidationReport::new).toList();
    submissionValidationContext.addClaimReports(claimValidationErrors);
  }

  /**
   * Get data for all claims with READY_TO_PROCESS status in a submission.
   *
   * @param submission the submission
   * @return a list of claims in the submission that are ready for processing.
   */
  private List<ClaimResponse> getReadyToProcessClaims(SubmissionResponse submission) {
    if (submission.getClaims() == null) {
      return Collections.emptyList();
    }
    return submission.getClaims().stream()
        .filter(claim -> ClaimStatus.READY_TO_PROCESS.equals(claim.getStatus()))
        .map(SubmissionClaim::getClaimId)
        .map(
            claimId ->
                dataClaimsRestClient.getClaim(submission.getSubmissionId(), claimId).getBody())
        .toList();
  }
}
