package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Interface for a claim validator. Implementations should be annotated with @Component.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 */
public interface ClaimWithAreaOfLawValidator {

  /**
   * Validates a claim.
   *
   * @param claim the claim to validate
   * @param context the validation context to add errors to
   * @param areaOfLaw the area of law for the claim
   */
  void validate(
      final ClaimResponse claim, SubmissionValidationContext context, AreaOfLaw areaOfLaw);
}
