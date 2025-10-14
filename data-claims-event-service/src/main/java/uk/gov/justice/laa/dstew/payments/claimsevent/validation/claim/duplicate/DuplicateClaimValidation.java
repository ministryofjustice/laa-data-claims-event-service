package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;
import java.util.function.Predicate;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;

/** Base class for duplicate claim validation. */
public abstract class DuplicateClaimValidation {

  protected static final List<ClaimStatus> listOfNonInvalidStatus =
      List.of(ClaimStatus.READY_TO_PROCESS, ClaimStatus.VALID);

  protected final DataClaimsRestClient dataClaimsRestClient;

  protected DuplicateClaimValidation(DataClaimsRestClient dataClaimsRestClient) {
    this.dataClaimsRestClient = dataClaimsRestClient;
  }

  /**
   * Filter the claims in the submission to contain all claims except the one currently under
   * validation.
   *
   * @param currentClaim the claim to filter
   * @param submissionClaims the list of claims in the submission
   * @return a filtered list of claims in the submission, excluding the given claim
   */
  protected List<ClaimResponse> filterCurrentClaimWithValidStatusAndWithinPeriod(
      ClaimResponse currentClaim, List<ClaimResponse> submissionClaims) {
    return submissionClaims.stream()
        .filter(submissionClaim -> submissionClaim != currentClaim)
        .filter(submissionClaim -> listOfNonInvalidStatus.contains(submissionClaim.getStatus()))
        .toList();
  }

  /**
   * Find duplicates in a list of claims, using a predicate to determine whether the claim is a
   * duplicate.
   *
   * @param otherClaimsWithNonInvalidStatus the list of claims to compare against
   * @param duplicatePredicate predicate to determine whether a claim is a duplicate
   * @return the list of duplicate claims, as determined by the given predicate
   */
  protected List<ClaimResponse> getDuplicateClaimsInCurrentSubmission(
      List<ClaimResponse> otherClaimsWithNonInvalidStatus,
      Predicate<ClaimResponse> duplicatePredicate) {
    return otherClaimsWithNonInvalidStatus.stream().filter(duplicatePredicate).toList();
  }

  /**
   * Search for duplicates in all other claims made by this office, with the same office code, fee
   * code, and unique file number. Ignore claims within this submission as they are verified
   * separately.
   *
   * @param officeCode the unique identifier for the office
   * @param feeCode the fee code
   * @param uniqueFileNumber the unique file number for the claim
   * @param uniqueClientNumber the unique client number for the claim
   * @param submissionClaims list of claims in the current submission
   * @return a list of duplicates across other claims by the same office
   */
  protected List<ClaimResponse> getDuplicateClaimsInPreviousSubmission(
      final String officeCode,
      final String feeCode,
      final String uniqueFileNumber,
      final String uniqueClientNumber,
      final List<ClaimResponse> submissionClaims) {
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
            uniqueClientNumber,
            List.of(ClaimStatus.READY_TO_PROCESS, ClaimStatus.VALID),
            null)
        .getBody()
        .getContent()
        .stream()
        .filter(prevClaim -> !submissionClaims.contains(prevClaim))
        .toList();
  }
}
