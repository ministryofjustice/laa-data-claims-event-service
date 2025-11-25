package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.util.Map;
import java.util.Set;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

/**
 * Interface for a claim validator. Implementations should be annotated with @Component.
 *
 * @author Jamie Briggs
 */
public abstract class RegexClaimValidator extends SchemaValidator implements ClaimValidator {

  /**
   * Constructor.
   *
   * @param schemaValidationErrorMessages schema validation error messages.
   */
  protected RegexClaimValidator(
      Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages) {
    super(schemaValidationErrorMessages);
  }

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
  protected void validateFieldWithRegex(
      ClaimResponse claim,
      AreaOfLaw areaOfLaw,
      String fieldValue,
      String fieldName,
      String regex,
      SubmissionValidationContext context) {
    String technicalMessage =
        String.format(
            "%s (%s): does not match the regex pattern %s (provided value: %s)",
            fieldName, areaOfLaw, regex, fieldValue);
    String displayMessage =
        getValidationErrorMessageFromSchema(fieldName, technicalMessage, areaOfLaw);
    if (regex != null && fieldValue != null && !fieldValue.matches(regex)) {
      context.addClaimError(claim.getId(), technicalMessage, displayMessage, EVENT_SERVICE);
    }
  }
}
