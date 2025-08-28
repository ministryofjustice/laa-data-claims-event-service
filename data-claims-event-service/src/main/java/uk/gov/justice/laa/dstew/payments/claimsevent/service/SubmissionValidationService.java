package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetSubmission200ResponseClaimsInner;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetSubmission200ResponseClaimsInner.StatusEnum;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionFields;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
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
  private final SubmissionValidationContext submissionValidationContext;

  /**
   * Validates a claim submission.
   *
   * @param submission the claim submission to validate
   */
  public void validateSubmission(GetSubmission200Response submission) {
    SubmissionFields submissionFields = submission.getSubmission();
    if (submissionFields == null) {
      throw new SubmissionValidationException("Submission is null");
    }

    UUID submissionId = submissionFields.getSubmissionId();

    verifySubmissionStatus(submissionId, submissionFields.getStatus());

    List<ClaimFields> claims = getReadyToProcessClaims(submission);

    initialiseValidationContext(claims);

    validateNilSubmission(submission);

    String officeCode = submissionFields.getOfficeAccountNumber();
    String areaOfLaw = submissionFields.getAreaOfLaw();
    List<String> providerCategoriesOfLaw = getProviderCategoriesOfLaw(officeCode, areaOfLaw);
    validateProviderContract(providerCategoriesOfLaw);

    claimValidationService.validateClaims(claims, providerCategoriesOfLaw);

    // TODO: Send through all claim errors in the patch request.
    updateClaims(submissionId, claims);

    // TODO: Verify all claims have been validated, and update submission status to
    //  VALIDATION_SUCCEEDED or VALIDATION_FAILED
    //  If unvalidated claims remain, re-queue message.
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

  private void validateNilSubmission(GetSubmission200Response submission) {
    if (Boolean.TRUE.equals(submission.getSubmission().getIsNilSubmission())) {
      if (submission.getClaims() != null && !submission.getClaims().isEmpty()) {
        submissionValidationContext.addToAllClaimReports(
            ClaimValidationError.INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS);
      }
    } else if (Boolean.FALSE.equals(submission.getSubmission().getIsNilSubmission())) {
      if (submission.getClaims() == null || submission.getClaims().isEmpty()) {
        throw new SubmissionValidationException(
            "Submission is not marked as nil submission, but does not contain any claims");
      }
    }
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

  private void validateProviderContract(List<String> providerCategoriesOfLaw) {
    if (providerCategoriesOfLaw.isEmpty()) {
      submissionValidationContext.addToAllClaimReports(
          ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);
    }
  }

  private void updateClaims(UUID submissionId, List<ClaimFields> claims) {
    claims.forEach(
        claim -> {
          ClaimPatch claimPatch =
              ClaimPatch.builder().id(claim.getId()).status(getClaimStatus(claim.getId())).build();
          dataClaimsRestClient.updateClaim(
              submissionId, UUID.fromString(claim.getId()), claimPatch);
        });
  }

  private ClaimStatus getClaimStatus(String claimId) {
    if (submissionValidationContext.hasErrors(claimId)) {
      return ClaimStatus.INVALID;
    } else {
      return ClaimStatus.VALID;
    }
  }

  private void initialiseValidationContext(List<ClaimFields> claims) {
    List<ClaimValidationReport> claimValidationErrors =
        claims.stream().map(ClaimFields::getId).map(ClaimValidationReport::new).toList();
    submissionValidationContext.addClaimReports(claimValidationErrors);
  }

  /**
   * Get data for all claims with READY_TO_PROCESS status in a submission.
   *
   * @param submission the submission
   * @return a list of claims in the submission that are ready for processing.
   */
  private List<ClaimFields> getReadyToProcessClaims(GetSubmission200Response submission) {
    if (submission.getClaims() == null) {
      return Collections.emptyList();
    }
    return submission.getClaims().stream()
        .filter(claim -> StatusEnum.READY_TO_PROCESS.equals(claim.getStatus()))
        .map(GetSubmission200ResponseClaimsInner::getClaimId)
        .map(
            claimId ->
                dataClaimsRestClient
                    .getClaim(submission.getSubmission().getSubmissionId(), claimId)
                    .getBody())
        .toList();
  }
}
