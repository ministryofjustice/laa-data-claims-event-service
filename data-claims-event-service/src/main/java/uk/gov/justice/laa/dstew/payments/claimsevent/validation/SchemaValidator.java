package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

/**
 * Abstract class for validating objects against predefined JSON schemas.
 *
 * @author Jamie Briggs
 */
public abstract class SchemaValidator {

  private final Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages;

  /**
   * Constructor.
   *
   * @param schemaValidationErrorMessages schema validation error messages.
   */
  protected SchemaValidator(
      Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages) {
    this.schemaValidationErrorMessages = schemaValidationErrorMessages;
  }

  /**
   * Gets the validation error message from the schema.
   *
   * @param field the field to get the error message for.
   * @param areaOfLaw the area of law to get the error message for.
   * @param defaultMessage the default message to use if no error message is found.
   * @return the validation error message.
   */
  protected String getValidationErrorMessageFromSchema(
      final String field, final String defaultMessage, final AreaOfLaw areaOfLaw) {
    return Optional.ofNullable(schemaValidationErrorMessages.get(field))
        .orElse(new HashSet<>())
        .stream()
        .filter(
            validationErrorMessage ->
                Objects.equals(validationErrorMessage.key(), areaOfLaw.getValue())
                    || Objects.equals(validationErrorMessage.key(), "ALL"))
        .map(ValidationErrorMessage::value)
        .findFirst()
        .orElse(defaultMessage);
  }
}
