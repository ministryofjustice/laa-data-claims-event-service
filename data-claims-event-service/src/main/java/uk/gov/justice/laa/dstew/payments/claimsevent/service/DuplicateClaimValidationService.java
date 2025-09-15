package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Service responsible for validating whether a claim is a duplicate. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateClaimValidationService {

  private final DataClaimsRestClient dataClaimsRestClient;

  /**
   * Validates whether a claim has been previously submitted or has been submitted with the same
   * details within the same submission, and is therefore a duplicate.
   *
   * @param claim the submitted claim
   */
  public void validateDuplicateClaims(
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      String areaOfLaw,
      String officeCode,
      SubmissionValidationContext context) {
    log.debug("Validating duplicates for claim {}", claim.getId());
    if (!context.isFlaggedForRetry(claim.getId())) {
      List<ClaimResponse> claimsToCompare = getOtherClaimsInSubmission(claim, submissionClaims);

      List<ClaimResponse> submissionDuplicateClaims = new ArrayList<>();
      List<ClaimResponse> officeDuplicateClaims = new ArrayList<>();

      if ("CRIME_LOWER".equals(areaOfLaw)) {
        String feeCode = claim.getFeeCode();
        String uniqueFileNumber = claim.getUniqueFileNumber();
        List<String> submissionClaimIds =
            submissionClaims.stream().map(ClaimResponse::getId).toList();

        submissionDuplicateClaims =
            findDuplicates(
                claimsToCompare,
                claimToCompare ->
                    feeCode.equals(claimToCompare.getFeeCode())
                        && uniqueFileNumber.equals(claimToCompare.getUniqueFileNumber()));

        officeDuplicateClaims =
            searchOfficeClaims(officeCode, feeCode, uniqueFileNumber, submissionClaimIds);
      }

      if (!submissionDuplicateClaims.isEmpty()) {
        log.debug("Duplicate claims found in submission");
        logDuplicates(claim, submissionDuplicateClaims);
        context.addClaimError(
            claim.getId(), ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION);
      }
      if (officeDuplicateClaims != null && !officeDuplicateClaims.isEmpty()) {
        log.debug("Duplicate claims found in another submission for this office");
        logDuplicates(claim, officeDuplicateClaims);
        context.addClaimError(
            claim.getId(), ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
      }
    }
    log.debug("Duplicate validation completed for claim {}", claim.getId());
  }

  /**
   * Filter the claims in the submission to contain all claims except the one currently under
   * validation.
   *
   * @param claim the claim to filter
   * @param submissionClaims the list of claims in the submission
   * @return a filtered list of claims in the submission, excluding the given claim
   */
  private List<ClaimResponse> getOtherClaimsInSubmission(
      ClaimResponse claim, List<ClaimResponse> submissionClaims) {
    return submissionClaims.stream()
        .filter(submissionClaim -> submissionClaim != claim)
        .filter(
            submissionClaim ->
                List.of(ClaimStatus.READY_TO_PROCESS, ClaimStatus.VALID)
                    .contains(submissionClaim.getStatus()))
        .toList();
  }

  /**
   * Search for duplicates in all other claims made by this office, with the same office code, fee
   * code, and unique file number. Ignore claims within this submission as they are verified
   * separately.
   *
   * @param officeCode the unique identifier for the office
   * @param feeCode the fee code
   * @param uniqueFileNumber the unique file number for the claim
   * @param submissionClaimIds the IDs of the other claims in the submission
   * @return a list of duplicates across other claims by the same office
   */
  private List<ClaimResponse> searchOfficeClaims(
      String officeCode, String feeCode, String uniqueFileNumber, List<String> submissionClaimIds) {
    return dataClaimsRestClient
        .getClaims(
            officeCode,
            null,
            List.of(
                SubmissionStatus.CREATED,
                SubmissionStatus.VALIDATION_IN_PROGRESS,
                SubmissionStatus.READY_FOR_VALIDATION),
            feeCode,
            uniqueFileNumber,
            null,
            List.of(ClaimStatus.VALID, ClaimStatus.READY_TO_PROCESS))
        .getBody()
        .getContent()
        .stream()
        .filter(otherClaim -> !submissionClaimIds.contains(otherClaim.getId()))
        .toList();
  }

  private void logDuplicates(ClaimResponse claim, List<ClaimResponse> duplicateClaims) {
    String csvDuplicateClaimIds =
        duplicateClaims.stream().map(ClaimResponse::getId).collect(Collectors.joining(","));
    log.debug(
        "{} duplicate claims found matching claim {}. Duplicates: {}",
        duplicateClaims.size(),
        claim.getId(),
        csvDuplicateClaimIds);
  }

  /**
   * Find duplicates in a list of claims, using a predicate to determine whether the claim is a
   * duplicate.
   *
   * @param claimsToCompare the list of claims to compare against
   * @param duplicatePredicate predicate to determine whether a claim is a duplicate
   * @return the list of duplicate claims, as determined by the given predicate
   */
  private List<ClaimResponse> findDuplicates(
      List<ClaimResponse> claimsToCompare, Predicate<ClaimResponse> duplicatePredicate) {
    return claimsToCompare.stream().filter(duplicatePredicate).toList();
  }
}
