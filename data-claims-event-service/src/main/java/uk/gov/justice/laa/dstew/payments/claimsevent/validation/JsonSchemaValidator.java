package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;


/** Class responsible for validating objects against predefined JSON schemas. */
@Component
@RequiredArgsConstructor
public class JsonSchemaValidator {

  private final ObjectMapper mapper;

  // Map of schema names to JsonSchema objects
  private final Map<String, JsonSchema> schemas;
  //TODO: uncomment this when schemaValidationErrorMessages is used
  //private final Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages;

  /**
   * Validate an object against the schema identified by schemaName.
   *
   * @param schemaName key in the schemas map
   * @param object any object that can be converted to JSON
   * @return list of enriched validation messages
   */
  public List<ValidationMessagePatch> validate(String schemaName, Object object) {
    JsonSchema schema = schemas.get(schemaName);
    schema.getValidators();
    if (schema == null) {
      throw new IllegalArgumentException("No schema registered for name: " + schemaName);
    }
    JsonNode data = mapper.valueToTree(object);

    return schema.validate(data).stream().map(vm -> toValidationMessagePatch(vm, data)).toList();
  }

  private ValidationMessagePatch toValidationMessagePatch(ValidationMessage vm, JsonNode data) {
    return new ValidationMessagePatch()
        .type(ValidationMessageType.ERROR)
        .source(EVENT_SERVICE)
        .displayMessage(enrichValidationMessage(data, vm))
        .technicalMessage(enrichValidationMessage(data, vm));
  }

  private String enrichValidationMessage(JsonNode data, ValidationMessage vm) {
    String message = vm.getMessage();
    String field = vm.getMessage().split(":")[0].replaceFirst("^\\$\\.", "");
    JsonNode valueNode = data.get(field);
    String value = valueNode == null || valueNode.isNull() ? "null" : valueNode.asText();
    //TODO: Use getValidationErrorMessageFromSchema
   /* return getValidationErrorMessageFromSchema(field)
            .orElse(
                    String.format(
                            "%s: %s (provided value: %s)",
                            field, message.substring(message.indexOf(':') + 1).trim(), value));*/
    return String.format(
            "%s: %s (provided value: %s)",
            field, message.substring(message.indexOf(':') + 1).trim(), value);
  }

  //TODO: uncomment this method
  /*
  private Optional<String> getValidationErrorMessageFromSchema(final String field) {
    return schemaValidationErrorMessages.get(field).stream()
        .filter(validationErrorMessage -> Objects.equals(validationErrorMessage.key(), "ALL"))
        .map(ValidationErrorMessage::value)
        .findFirst();
  }*/
}
