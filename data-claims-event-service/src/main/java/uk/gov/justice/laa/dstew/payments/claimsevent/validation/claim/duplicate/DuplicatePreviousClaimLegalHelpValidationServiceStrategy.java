package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Validation service for legal help duplicate claims in the current submission. */
@Slf4j
@Service
public final class DuplicatePreviousClaimLegalHelpValidationServiceStrategy
    extends DuplicateClaimValidation implements LegalHelpDuplicateClaimValidationStrategy {

  @Autowired
  public DuplicatePreviousClaimLegalHelpValidationServiceStrategy(
      final DataClaimsRestClient dataClaimsRestClient) {
    super(dataClaimsRestClient);
  }

  @Override
  public void validateDuplicateClaims(
      final ClaimResponse currentClaim,
      final List<ClaimResponse> submissionClaims,
      final String officeCode,
      final SubmissionValidationContext context) {

    List<ClaimResponse> otherClaimsWithValidStatus =
        filterCurrentClaimWithValidStatusAndWithinPeriod(currentClaim, submissionClaims);

    List<ClaimResponse> duplicateClaimsInThisSubmission =
        getDuplicateClaimsInCurrentSubmission(
            otherClaimsWithValidStatus,
            duplicatePredicate ->
                Objects.equals(duplicatePredicate.getFeeCode(), currentClaim.getFeeCode())
                    && Objects.equals(
                        duplicatePredicate.getUniqueFileNumber(),
                        currentClaim.getUniqueFileNumber())
                    && Objects.equals(
                        duplicatePredicate.getUniqueClientNumber(),
                        currentClaim.getUniqueClientNumber()));

    // add an error against the currentClaim if there are any duplicates are found.
    if (!duplicateClaimsInThisSubmission.isEmpty()) {
      logDuplicates(currentClaim, duplicateClaimsInThisSubmission);
      context.addClaimError(
          currentClaim.getId(),
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION);
    }
  }
}
