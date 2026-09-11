package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Service responsible for validating whether a claim is a duplicate. */
@Slf4j
@Service
public final class DuplicateClaimCrimeLowerValidationServiceStrategy
    extends DuplicateClaimValidation {

  @Autowired
  public DuplicateClaimCrimeLowerValidationServiceStrategy(
      DataClaimsRestClient dataClaimsRestClient) {
    super(dataClaimsRestClient);
  }

  @Override
  public void validateDuplicateClaims(
      ClaimResponse currentClaim,
      List<ClaimResponse> submissionClaims,
      String officeCode,
      SubmissionValidationContext context) {

    log.debug(
        "[{}] Validating duplicates for claim {}",
        getClass().getSimpleName(),
        currentClaim.getId());

    if ("PROD".equals(currentClaim.getFeeCode())) {
      // Skipping PRD duplicate check. This is because PROD fee code do not have a unique
      // identifier and client details are not mandatory for this fee code. This was originally
      // implemented but then removed as part of BC-418. Please check
      // https://github.com/ministryofjustice/laa-data-claims-event-service/releases/tag/0.0.121
      // for the previous implementation of this check.
      log.debug("Fee code is PROD, skipping duplicate check for claim {}", currentClaim.getId());
      return;
    }

    // Get all claims from the API by officeCode, feeCode and uniqueFileNumber.
    List<ClaimResponse> duplicateClaims =
        getDuplicateClaims(
            officeCode, currentClaim.getFeeCode(), currentClaim.getUniqueFileNumber(), null);

    // Filter the claims to find duplicates in the current submission.
    List<ClaimResponse> submissionDuplicateClaims =
        filterDuplicateClaimsInSameSubmission(currentClaim, duplicateClaims);
    findDuplicateClaims(
        currentClaim,
        submissionDuplicateClaims,
        ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION,
        context);

    // Filter the claims to find duplicates in previous submissions.
    List<ClaimResponse> officeDuplicateClaims =
        filterDuplicateClaimsInPreviousSubmission(currentClaim, duplicateClaims);
    findDuplicateClaims(
        currentClaim,
        officeDuplicateClaims,
        ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION,
        context);

    log.debug(
        "[{}] Duplicate validation completed for claim {}",
        getClass().getSimpleName(),
        currentClaim.getId());
  }

  @Override
  public List<String> compatibleStrategies() {
    return List.of(AreaOfLaw.CRIME_LOWER.getValue());
  }
}
