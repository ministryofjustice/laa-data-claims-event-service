package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

@DisplayName("Schema validator test")
class SchemaValidatorTest {

  private static final String LEGAL_HELP_AOL = "LEGAL HELP";
  private static final String CRIME_LOWER_AOL = "CRIME LOWER";
  private static final String MEDIATION_AOL = "MEDIATION";

  /** Test schema validator. Extends the abstract class we are wanting to test. */
  public class TestSchemaValidator extends SchemaValidator {

    /**
     * Constructor.
     *
     * @param schemaValidationErrorMessages schema validation error messages.
     */
    protected TestSchemaValidator(
        Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages) {
      super(schemaValidationErrorMessages);
    }
  }

  @ParameterizedTest
  @EnumSource(value = AreaOfLaw.class)
  @DisplayName("Should return default message when no validation message exists")
  void shouldReturnDefaultMessageWhenNoValidationMessageExists(AreaOfLaw areaOfLaw) {
    // Given
    TestSchemaValidator schemaValidator = new TestSchemaValidator(new HashMap<>());
    // When
    String result =
        schemaValidator.getValidationErrorMessageFromSchema("field", "Default message", areaOfLaw);
    // Then
    assertThat(result).isEqualTo("Default message");
  }

  @ParameterizedTest
  @EnumSource(value = AreaOfLaw.class)
  @DisplayName("Should return validation message when validation message exists")
  void shouldReturnValidationMessageWhenValidationMessageExists(AreaOfLaw areaOfLaw) {
    // Given
    HashMap<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages = new HashMap<>();
    HashSet<ValidationErrorMessage> possibleMessages = new HashSet<>();
    possibleMessages.add(new ValidationErrorMessage("ALL", "Schema message"));
    schemaValidationErrorMessages.put("field", possibleMessages);
    TestSchemaValidator schemaValidator = new TestSchemaValidator(schemaValidationErrorMessages);
    // When
    String result =
        schemaValidator.getValidationErrorMessageFromSchema("field", "Default message", areaOfLaw);
    // Then
    assertThat(result).isEqualTo("Schema message");
  }

  @Test
  @DisplayName("Should return validation message when validation message exists - LEGAL HELP")
  void shouldReturnValidationMessageWhenValidationMessageExistsLegalHelp() {
    // Given
    HashMap<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages = new HashMap<>();
    HashSet<ValidationErrorMessage> possibleMessages = new HashSet<>();
    possibleMessages.add(
        new ValidationErrorMessage(LEGAL_HELP_AOL, LEGAL_HELP_AOL + " - Schema message"));
    schemaValidationErrorMessages.put("field", possibleMessages);
    TestSchemaValidator schemaValidator = new TestSchemaValidator(schemaValidationErrorMessages);
    // When
    String result =
        schemaValidator.getValidationErrorMessageFromSchema(
            "field", "Default message", AreaOfLaw.LEGAL_HELP);
    // Then
    assertThat(result).isEqualTo(LEGAL_HELP_AOL + " - Schema message");
  }

  @Test
  @DisplayName("Should return validation message when validation message exists - CRIME LOWER")
  void shouldReturnValidationMessageWhenValidationMessageExistsCrimeLower() {
    // Given
    HashMap<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages = new HashMap<>();
    HashSet<ValidationErrorMessage> possibleMessages = new HashSet<>();
    possibleMessages.add(
        new ValidationErrorMessage(CRIME_LOWER_AOL, CRIME_LOWER_AOL + " - Schema message"));
    schemaValidationErrorMessages.put("field", possibleMessages);
    TestSchemaValidator schemaValidator = new TestSchemaValidator(schemaValidationErrorMessages);
    // When
    String result =
        schemaValidator.getValidationErrorMessageFromSchema(
            "field", "Default message", AreaOfLaw.CRIME_LOWER);
    // Then
    assertThat(result).isEqualTo(CRIME_LOWER_AOL + " - Schema message");
  }

  @Test
  @DisplayName("Should return validation message when validation message exists - MEDIATION")
  void shouldReturnValidationMessageWhenValidationMessageExistsMediation() {
    // Given
    HashMap<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages = new HashMap<>();
    HashSet<ValidationErrorMessage> possibleMessages = new HashSet<>();
    possibleMessages.add(
        new ValidationErrorMessage(MEDIATION_AOL, MEDIATION_AOL + " - Schema message"));
    schemaValidationErrorMessages.put("field", possibleMessages);
    TestSchemaValidator schemaValidator = new TestSchemaValidator(schemaValidationErrorMessages);
    // When
    String result =
        schemaValidator.getValidationErrorMessageFromSchema(
            "field", "Default message", AreaOfLaw.MEDIATION);
    // Then
    assertThat(result).isEqualTo(MEDIATION_AOL + " - Schema message");
  }
}
