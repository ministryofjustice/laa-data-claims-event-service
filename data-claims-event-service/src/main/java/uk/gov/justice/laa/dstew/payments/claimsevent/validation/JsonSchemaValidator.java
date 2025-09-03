package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Class responsible for validating objects against predefined JSON schemas.
 */
@Component
@RequiredArgsConstructor
public class JsonSchemaValidator {

  private final ObjectMapper mapper;

  // Map of schema names to JsonSchema objects
  private final Map<String, JsonSchema> schemas;

  /**
   * Validate an object against the schema identified by schemaName.
   *
   * @param schemaName key in the schemas map
   * @param object any object that can be converted to JSON
   * @return list of enriched validation messages
   */
  public List<String> validate(String schemaName, Object object) {
    JsonSchema schema = schemas.get(schemaName);
    if (schema == null) {
      throw new IllegalArgumentException("No schema registered for name: " + schemaName);
    }
    JsonNode data = mapper.valueToTree(object);
    return schema.validate(data)
        .stream()
        .map(vm -> enrichValidationMessage(data, vm))
        .toList();
  }

  private String enrichValidationMessage(JsonNode data, ValidationMessage vm) {
    String message = vm.getMessage();
    String field = message.split(":")[0].replaceFirst("^\\$\\.", "");
    JsonNode valueNode = data.get(field);
    String value = valueNode == null || valueNode.isNull() ? "null" : valueNode.asText();

    return String.format("%s: %s (provided value: %s)",
        field,
        message.substring(message.indexOf(":") + 1).trim(),
        value);
  }
}