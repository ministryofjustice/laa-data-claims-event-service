package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

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
  void validate(final ClaimResponse claim, SubmissionValidationContext context, String areaOfLaw);

  /**
   * Validates a specific field of a claim using a provided regex pattern. If the field value does
   * not match the regex, an error is added to the validation context.
   *
   * @param claim the claim containing the field to validate
   * @param areaOfLaw the area of law associated with the claim
   * @param fieldValue the value of the field to validate
   * @param fieldName the name of the field being validated
   * @param regex the regular expression pattern used for validation
   * @param context the validation context used to collect validation errors
   */
  default void validateFieldWithRegex(
      ClaimResponse claim,
      String areaOfLaw,
      String fieldValue,
      String fieldName,
      String regex,
      SubmissionValidationContext context) {
    if (regex != null && fieldValue != null && !fieldValue.matches(regex)) {
      context.addClaimError(
          claim.getId(),
          String.format(
              "%s (%s): does not match the regex pattern %s (provided value: %s)",
              fieldName, areaOfLaw, regex, fieldValue),
          EVENT_SERVICE);
    }
  }
}
