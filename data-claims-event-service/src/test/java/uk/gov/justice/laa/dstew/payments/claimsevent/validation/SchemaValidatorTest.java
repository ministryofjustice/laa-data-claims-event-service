package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

@DisplayName("Schema validator test")
class SchemaValidatorTest {

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

  @Test
  @DisplayName("Should return default message when no validation message exists")
  void shouldReturnDefaultMessageWhenNoValidationMessageExists() {
    // Given
    TestSchemaValidator schemaValidator = new TestSchemaValidator(new HashMap<>());
    // When
    String result = schemaValidator.getValidationErrorMessageFromSchema("field", "Default message");
    // Then
    assertThat(result).isEqualTo("Default message");
  }

  @Test
  @DisplayName("Should return validation message when validation message exists")
  void shouldReturnValidationMessageWhenValidationMessageExists() {
    // Given
    HashMap<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages = new HashMap<>();
    HashSet<ValidationErrorMessage> possibleMessages = new HashSet<>();
    possibleMessages.add(new ValidationErrorMessage("ALL", "Schema message"));
    schemaValidationErrorMessages.put("field", possibleMessages);
    TestSchemaValidator schemaValidator = new TestSchemaValidator(schemaValidationErrorMessages);
    // When
    String result = schemaValidator.getValidationErrorMessageFromSchema("field", "Default message");
    // Then
    assertThat(result).isEqualTo("Schema message");
  }

  @ParameterizedTest
  @ValueSource(strings = {"CIVIL", "LEGAL HELP"})
  @DisplayName("Should return validation message when validation message exists - CIVIL")
  void shouldReturnValidationMessageWhenValidationMessageExistsCivil(String areaOfLawCode) {
    // Given
    HashMap<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages = new HashMap<>();
    HashSet<ValidationErrorMessage> possibleMessages = new HashSet<>();
    possibleMessages.add(new ValidationErrorMessage("CIVIL", "CIVIL - Schema message"));
    schemaValidationErrorMessages.put("field", possibleMessages);
    TestSchemaValidator schemaValidator = new TestSchemaValidator(schemaValidationErrorMessages);
    // When
    String result =
        schemaValidator.getValidationErrorMessageFromSchema(
            "field", areaOfLawCode, "Default message");
    // Then
    assertThat(result).isEqualTo("CIVIL - Schema message");
  }

  @ParameterizedTest
  @ValueSource(strings = {"CRIME", "CRIME LOWER"})
  @DisplayName("Should return validation message when validation message exists - CRIME")
  void shouldReturnValidationMessageWhenValidationMessageExistsCrime(String areaOfLawCode) {
    // Given
    HashMap<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages = new HashMap<>();
    HashSet<ValidationErrorMessage> possibleMessages = new HashSet<>();
    possibleMessages.add(new ValidationErrorMessage("CRIME", "CRIME - Schema message"));
    schemaValidationErrorMessages.put("field", possibleMessages);
    TestSchemaValidator schemaValidator = new TestSchemaValidator(schemaValidationErrorMessages);
    // When
    String result =
        schemaValidator.getValidationErrorMessageFromSchema(
            "field", areaOfLawCode, "Default message");
    // Then
    assertThat(result).isEqualTo("CRIME - Schema message");
  }

  @Test
  @DisplayName("Should return validation message when validation message exists - MEDIATION")
  void shouldReturnValidationMessageWhenValidationMessageExistsMediation() {
    // Given
    HashMap<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages = new HashMap<>();
    HashSet<ValidationErrorMessage> possibleMessages = new HashSet<>();
    possibleMessages.add(new ValidationErrorMessage("MEDIATION", "MEDIATION - Schema message"));
    schemaValidationErrorMessages.put("field", possibleMessages);
    TestSchemaValidator schemaValidator = new TestSchemaValidator(schemaValidationErrorMessages);
    // When
    String result =
        schemaValidator.getValidationErrorMessageFromSchema(
            "field", "MEDIATION", "Default message");
    // Then
    assertThat(result).isEqualTo("MEDIATION - Schema message");
  }
}
