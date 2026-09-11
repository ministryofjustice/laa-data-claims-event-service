package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Service interface for validating duplicate claims. */
public interface DuplicateClaimValidationStrategy {

  default void validateDuplicateClaims(
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      String officeCode,
      SubmissionValidationContext context) {
    // Default implementation does nothing, overridden methods will be called.
  }

  default void validateDuplicateClaims(
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      String officeCode,
      SubmissionValidationContext context,
      String feeCalculationType) {
    validateDuplicateClaims(claim, submissionClaims, officeCode, context);
  }

  /**
   * Get the list of compatible area of laws for this strategy.
   *
   * @return List of compatible area of laws.
   */
  default List<String> compatibleStrategies() {
    return List.of();
  }
}
