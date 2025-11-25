package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.StringCaseUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

/** Class responsible for validating objects against predefined JSON schemas. */
@Component
public class JsonSchemaValidator extends SchemaValidator {

  public static final String REQUIRED = "required";
  private final ObjectMapper mapper;

  // Map of schema names to JsonSchema objects
  private final Map<String, JsonSchema> schemas;

  /**
   * Constructs JsonSchemaValidator.
   *
   * @param mapper Object mapper.
   * @param schemas map of schema names to JsonSchema objects.
   * @param schemaValidationErrorMessages map of schema names to error messages.
   */
  public JsonSchemaValidator(
      final ObjectMapper mapper,
      final Map<String, JsonSchema> schemas,
      final Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages) {
    super(schemaValidationErrorMessages);
    this.mapper = mapper;
    this.schemas = schemas;
  }

  /**
   * Validate an object against the schema identified by schemaName.
   *
   * @param schemaName key in the schemas map
   * @param object any object that can be converted to JSON
   * @return list of enriched validation messages
   */
  public List<ValidationMessagePatch> validate(
      final String schemaName, final Object object, final AreaOfLaw areaOfLaw) {
    JsonSchema schema = schemas.get(schemaName);
    schema.getValidators();

    JsonNode data = mapper.valueToTree(object);

    Set<ValidationMessage> validationMessages = schema.validate(data);
    Map<String, String> fieldToTechnicalMessage =
        groupTechnicalMessageByField(validationMessages, data);

    return new HashSet<>(
            validationMessages.stream()
                .map(
                    vm -> {
                      String technicalMessage = fieldToTechnicalMessage.get(getFieldName(vm));
                      String displayMessage = getDisplayMessage(vm, technicalMessage, areaOfLaw);
                      return toValidationMessagePatch(vm, technicalMessage, displayMessage);
                    })
                .toList())
        .stream().toList();
  }

  private ValidationMessagePatch toValidationMessagePatch(
      final ValidationMessage vm, final String technicalMessage, final String displayMessage) {
    return new ValidationMessagePatch()
        .type(ValidationMessageType.ERROR)
        .source(EVENT_SERVICE)
        .displayMessage(displayMessage)
        .technicalMessage(technicalMessage);
  }

  private String getTechnicalMessage(final JsonNode data, final ValidationMessage vm) {
    String message = vm.getMessage();
    String field = getFieldName(vm);
    JsonNode valueNode = data.get(field);
    String value = valueNode == null || valueNode.isNull() ? "null" : valueNode.asText();
    return String.format(
        "%s: %s (provided value: %s)",
        field, message.substring(message.indexOf(':') + 1).trim(), value);
  }

  private String getDisplayMessage(
      final ValidationMessage vm, final String defaultMessage, final AreaOfLaw areaOfLaw) {
    return REQUIRED.equals(vm.getType())
        ? String.format("%s is required", StringCaseUtil.toTitleCase(vm.getProperty()))
        : getValidationErrorMessageFromSchema(getFieldName(vm), defaultMessage, areaOfLaw);
  }

  private String getFieldName(final ValidationMessage vm) {
    return vm.getMessage().split(":")[0].replaceFirst("^\\$\\.", "");
  }

  /**
   * Groups technical validation messages by their associated fields. Each field will map to a
   * combined technical message string if multiple validation messages are associated with the same
   * field.
   *
   * @param validationMessages a set of validation messages to be processed
   * @param data the JSON data node containing field values
   * @return a map where keys are field names and values are combined technical messages
   */
  private Map<String, String> groupTechnicalMessageByField(
      final Set<ValidationMessage> validationMessages, final JsonNode data) {
    Map<String, String> fieldToTechnicalMessage = new HashMap<>();
    validationMessages.forEach(
        vm -> {
          String fieldName = getFieldName(vm);
          String technicalMessage = getTechnicalMessage(data, vm);
          fieldToTechnicalMessage.merge(
              fieldName, technicalMessage, (existingMsg, newMsg) -> existingMsg + " : " + newMsg);
        });
    return fieldToTechnicalMessage;
  }
}
