package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

/**
 * Configuration class for setting up and managing JSON Schema validation. This class loads JSON
 * Schemas, manages their lifecycle, and provides them as beans for use across the application. The
 * configuration relies on the Jackson ObjectMapper for deserialization of schema JSON files, with
 * null values excluded during serialization by default.
 */
@Configuration
public class SchemaValidationConfig {

  private final ObjectMapper mapper;

  private final Resource resourceFile;

  /**
   * Constructs a new {@code SchemaValidationConfig} with the given {@link ObjectMapper}.
   *
   * <p>The provided mapper is configured to exclude {@code null} values during serialization by
   * setting its {@link com.fasterxml.jackson.annotation.JsonInclude.Include#NON_NULL} inclusion
   * policy. This ensures cleaner JSON output and avoids explicit {@code null} fields.
   *
   * @param mapper the Jackson {@link ObjectMapper} to configure and use for schema validation; must
   *     not be {@code null}
   */
  public SchemaValidationConfig(
      ObjectMapper mapper,
      @Value("classpath:schemas/claim-fields.schema.json") Resource resourceFile) {
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // exclude nulls
    this.mapper = mapper;
    this.resourceFile = resourceFile;
  }

  private JsonSchema loadSchema(String path) throws IOException {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    try (InputStream stream = getClass().getResourceAsStream(path)) {
      if (stream == null) {
        throw new IllegalStateException("Schema not found: " + path);
      }
      JsonNode schemaNode = mapper.readTree(stream);
      return factory.getSchema(schemaNode);
    }
  }

  /**
   * Provides a map of JSON Schemas to be used for validation purposes. Schemas are loaded as beans
   * and can be accessed by their identifiers.
   *
   * @return A map where the keys are schema identifiers (e.g., "submission", "claim") and the
   *     values are instances of {@link JsonSchema} representing the loaded schemas.
   * @throws IOException if an error occurs while loading the JSON Schemas.
   */
  @Bean
  public Map<String, JsonSchema> jsonSchemas() throws IOException {
    return Map.of(
        "submission", submissionSchema(),
        "claim", claimSchema()

        // Add more schemas here if needed
        );
  }

  /**
   * Generates a map of schema validation error messages based on a JSON schema's validation error
   * definitions. The method reads a schema file, parses its fields, and collects validation error
   * messages for each field where defined.
   *
   * @return A map where the keys are field names from the JSON schema and the values are lists of
   *     {@link ValidationErrorMessage} objects representing validation error messages for each
   *     corresponding field. If a field does not have validation error messages, it is excluded
   *     from the map.
   * @throws IOException If an error occurs while reading or parsing the schema file.
   */
  @Bean
  public Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages()
      throws IOException {
    // TODO: Need to tidy up this messy code
    JsonNode schemaNode =
        mapper.readTree(
            (Files.readString(resourceFile.getFile().toPath(), StandardCharsets.UTF_8)));

    // Get the properties node which contains all fields
    JsonNode propertiesNode = schemaNode.get("properties");

    Map<String, Set<ValidationErrorMessage>> result = new HashMap<>();

    // Iterate through all fields in the schema
    propertiesNode
        .fields()
        .forEachRemaining(
            field -> {
              String fieldName = field.getKey();
              JsonNode fieldNode = field.getValue();

              // Check if the field has validation error messages
              JsonNode messagesNode = fieldNode.get("validationErrorMessages");
              if (messagesNode != null && messagesNode.isArray()) {
                try {
                  // Convert the messages array to List<ValidationErrorMessage>
                  Set<ValidationErrorMessage> messages =
                      mapper.convertValue(messagesNode, new TypeReference<>() {});
                  result.put(fieldName, messages);
                } catch (Exception e) {
                  throw new RuntimeException(
                      "Error parsing validation messages for field: " + fieldName, e);
                }
              }
            });

    return result;
  }

  public JsonSchema submissionSchema() throws IOException {
    return loadSchema("/schemas/submission-fields.schema.json");
  }

  public JsonSchema claimSchema() throws IOException {
    return loadSchema("/schemas/claim-fields.schema.json");
  }
}
