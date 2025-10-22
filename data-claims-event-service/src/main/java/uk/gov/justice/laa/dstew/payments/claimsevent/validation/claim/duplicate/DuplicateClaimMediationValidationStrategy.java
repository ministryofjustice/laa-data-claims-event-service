package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Duplicate claims Validation Mediation Strategy. */
@Service
@Slf4j
public class DuplicateClaimMediationValidationStrategy extends DuplicateClaimValidation
    implements MediationDuplicateClaimValidationStrategy {
  protected DuplicateClaimMediationValidationStrategy(DataClaimsRestClient dataClaimsRestClient) {
    super(dataClaimsRestClient);
  }

  @Override
  public void validateDuplicateClaims(
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      String officeCode,
      SubmissionValidationContext context) {
    List<ClaimResponse> claimsWithValidStatus =
        filterCurrentClaimWithValidStatusAndWithinPeriod(claim, submissionClaims);
    String feeCode = claim.getFeeCode();
    String uniqueCaseId = claim.getUniqueCaseId();

    List<ClaimResponse> submissionDuplicateClaims =
        getDuplicateClaimsInCurrentSubmission(
            claimsWithValidStatus,
            claimToCompare ->
                feeCode.equals(claimToCompare.getFeeCode())
                    && uniqueCaseId.equals(claimToCompare.getUniqueCaseId()));

    List<ClaimResponse> officeDuplicateClaims =
        getDuplicateClaimsInPreviousSubmission(
            officeCode, feeCode, null, null, uniqueCaseId, submissionClaims);

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
    log.debug("Duplicate validation completed for claim {}", claim.getId());
  }
}
