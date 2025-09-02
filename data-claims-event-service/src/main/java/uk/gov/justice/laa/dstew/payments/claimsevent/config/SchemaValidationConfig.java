package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up and managing JSON Schema validation.
 * This class loads JSON Schemas, manages their lifecycle, and provides them as beans for use
 * across the application.
 * The configuration relies on the Jackson ObjectMapper for deserialization of schema JSON files,
 * with null values excluded during serialization by default.
 */
@Configuration
public class SchemaValidationConfig {

  private final ObjectMapper mapper;

  public SchemaValidationConfig(ObjectMapper mapper) {
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // exclude nulls
    this.mapper = mapper;
  }

  private JsonSchema loadSchema(String path) throws IOException {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    try (InputStream stream = getClass().getResourceAsStream(path)) {
      if (stream == null) {
        throw new IllegalStateException("Schema not found: " + path);
      }
      JsonNode schemaNode = mapper.readTree(stream);
      return factory.getSchema(schemaNode);
    }
  }

  @Bean
  public Map<String, JsonSchema> jsonSchemas() throws IOException {
    return Map.of(
        "submission", submissionSchema(),
        "claim", claimSchema()

        // Add more schemas here if needed
    );
  }

  public JsonSchema submissionSchema() throws IOException {
    return loadSchema("/schemas/submission-fields.schema.json");
  }

  public JsonSchema claimSchema() throws IOException {
    return loadSchema("/schemas/claim-fields.schema.json");
  }

}