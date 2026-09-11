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

/** Validation service for legal help duplicate claims in the current submission. */
@Slf4j
@Service
public final class DuplicateClaimLegalHelpCurrentSubmissionValidationServiceStrategy
    extends DuplicateClaimValidation {

  @Autowired
  public DuplicateClaimLegalHelpCurrentSubmissionValidationServiceStrategy(
      final DataClaimsRestClient dataClaimsRestClient) {
    super(dataClaimsRestClient);
  }

  @Override
  public void validateDuplicateClaims(
      final ClaimResponse currentClaim,
      final List<ClaimResponse> submissionClaims,
      final String officeCode,
      final SubmissionValidationContext context) {

    log.debug(
        "[{}] Validating duplicates for claim {}",
        getClass().getSimpleName(),
        currentClaim.getId());

    // Get all claims from the API by officeCode, feeCode, uniqueFileNumber and uniqueClientNumber.
    List<ClaimResponse> duplicateClaims =
        getDuplicateClaims(
            officeCode,
            currentClaim.getFeeCode(),
            currentClaim.getUniqueFileNumber(),
            currentClaim.getUniqueClientNumber());

    // Filter the claims to find duplicates in the current submission.
    List<ClaimResponse> submissionDuplicateClaims =
        filterDuplicateClaimsInSameSubmission(currentClaim, duplicateClaims);
    findDuplicateClaims(
        currentClaim,
        submissionDuplicateClaims,
        ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION,
        context);

    log.debug(
        "[{}] Duplicate validation completed for claim {}",
        getClass().getSimpleName(),
        currentClaim.getId());
  }

  @Override
  public List<String> compatibleStrategies() {
    return List.of(AreaOfLaw.LEGAL_HELP.getValue());
  }
}
