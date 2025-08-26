package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetSubmission200ResponseClaimsInner;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetSubmission200ResponseClaimsInner.StatusEnum;
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
    if (submission.getSubmission() == null) {
      throw new SubmissionValidationException("Submission is null");
    }

    List<ClaimFields> claims = getReadyToProcessClaims(submission);

    initialiseValidationContext(claims);

    validateNilSubmission(submission);

    String officeCode = submission.getSubmission().getOfficeAccountNumber();
    String areaOfLaw = submission.getSubmission().getAreaOfLaw();
    List<String> providerCategoriesOfLaw = getProviderCategoriesOfLaw(officeCode, areaOfLaw);
    validateProviderContract(providerCategoriesOfLaw);

    claimValidationService.validateClaims(claims, providerCategoriesOfLaw);

    UUID submissionId = submission.getSubmission().getSubmissionId();
    updateClaims(submissionId, claims);
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
    List<Mono<Void>> updateRequests =
        claims.stream()
            .map(
                claim ->
                    ClaimPatch.builder()
                        .id(claim.getId())
                        .status(getClaimStatus(claim.getId()))
                        .build())
            .map(
                claimPatch ->
                    dataClaimsRestClient.updateClaim(
                        submissionId, UUID.fromString(claimPatch.getId()), claimPatch))
            .toList();

    Flux.merge(updateRequests).subscribe();
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
