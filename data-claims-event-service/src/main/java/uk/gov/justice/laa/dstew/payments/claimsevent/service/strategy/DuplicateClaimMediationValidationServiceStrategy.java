package uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Mediation Case, duplication checks. */
@Slf4j
@Service(StrategyTypes.MEDIATION)
public class DuplicateClaimMediationValidationServiceStrategy extends DuplicateClaimValidation
    implements DuplicateClaimValidationStrategy {

  @Autowired
  public DuplicateClaimMediationValidationServiceStrategy(
      final DataClaimsRestClient dataClaimsRestClient) {
    super(dataClaimsRestClient);
  }

  /**
   * Validate duplicate claims. Office x Fee Code x Unique Case ID.
   *
   * @param submittedClaim The claim to validate against.
   * @param submissionClaims List of claims in current submission.
   * @param officeCode Office code.
   * @param context Context.
   */
  @Override
  public void validateDuplicateClaims(
      ClaimResponse submittedClaim,
      List<ClaimResponse> submissionClaims,
      String officeCode,
      SubmissionValidationContext context) {
    log.debug("Validating duplicates for claim {}", submittedClaim.getId());

    List<ClaimResponse> claimsWithNonInvalidStatus =
        filterCurrentClaimWithNonInvalidStatus(submittedClaim, submissionClaims);

    List<ClaimResponse> duplicateClaimsInThisSubmission =
        getDuplicateClaimsInCurrentSubmission(
            claimsWithNonInvalidStatus,
            duplicatePredicate ->
                Objects.equals(duplicatePredicate.getFeeCode(), submittedClaim.getFeeCode())
                    && Objects.equals(
                        duplicatePredicate.getUniqueCaseId(), submittedClaim.getUniqueCaseId()));

    List<ClaimResponse> duplicateClaimsInPreviousSubmission =
        getDuplicateClaimsInPreviousSubmission(
            officeCode,
            submittedClaim.getFeeCode(),
            null,
            null,
            submittedClaim.getUniqueCaseId(),
            submissionClaims);

    duplicateClaimsInThisSubmission.forEach(
        duplicateClaim -> {
          logDuplicates(duplicateClaim, duplicateClaimsInThisSubmission);
          context.addClaimError(
              duplicateClaim.getId(),
              ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION);
        });

    duplicateClaimsInPreviousSubmission.forEach(
        duplicateClaim -> {
          logDuplicates(duplicateClaim, duplicateClaimsInPreviousSubmission);
          context.addClaimError(
              submittedClaim.getId(),
              ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
        });
  }

  /**
   * Log duplicates.
   *
   * @param claim The claim to log duplicates for.
   * @param duplicateClaims The list of duplicate claims.
   */
  @Override
  public void logDuplicates(ClaimResponse claim, List<ClaimResponse> duplicateClaims) {
    DuplicateClaimValidationStrategy.super.logDuplicates(claim, duplicateClaims);
  }
}
