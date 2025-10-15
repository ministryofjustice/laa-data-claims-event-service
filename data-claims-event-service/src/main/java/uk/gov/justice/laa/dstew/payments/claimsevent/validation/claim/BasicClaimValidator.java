package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Interface for a claim validator. Implementations should be annotated with @Component.
 *
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @author Jamie Briggs
 */
public interface BasicClaimValidator {

  /**
   * Validates a claim.
   *
   * @param claim the claim to validate
   * @param context the validation context to add errors to
   */
  void validate(final ClaimResponse claim, SubmissionValidationContext context);
}
