package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Service responsible for validating whether a claim is a duplicate. */
@Slf4j
@Service
public final class DuplicateClaimCrimeLowerValidationServiceStrategy
    extends DuplicateClaimValidation implements CrimeLowerDuplicateClaimValidationStrategy {

  @Autowired
  public DuplicateClaimCrimeLowerValidationServiceStrategy(
      DataClaimsRestClient dataClaimsRestClient) {
    super(dataClaimsRestClient);
  }

  @Override
  public void validateDuplicateClaims(
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      String officeCode,
      SubmissionValidationContext context) {
    log.debug("Validating duplicates for claim {}", claim.getId());
    if (!context.isFlaggedForRetry(claim.getId())) {
      List<ClaimResponse> claimsToCompare =
          filterCurrentClaimWithValidStatusAndWithinPeriod(claim, submissionClaims);

      String feeCode = claim.getFeeCode();
      String uniqueFileNumber = claim.getUniqueFileNumber();

      List<ClaimResponse> submissionDuplicateClaims =
          getDuplicateClaimsInCurrentSubmission(
              claimsToCompare,
              claimToCompare ->
                  feeCode.equals(claimToCompare.getFeeCode())
                      && uniqueFileNumber.equals(claimToCompare.getUniqueFileNumber()));

      List<ClaimResponse> officeDuplicateClaims =
          getDuplicateClaimsInPreviousSubmission(
              officeCode, feeCode, uniqueFileNumber, null, null, submissionClaims);

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
}
