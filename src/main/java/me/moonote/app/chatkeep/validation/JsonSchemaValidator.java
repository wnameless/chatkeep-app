package me.moonote.app.chatkeep.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JsonSchemaValidator {

  private final JsonSchema archiveSchema;
  private final JsonSchema metadataSchema;
  private final JsonSchema artifactSchema;
  private final JsonSchema attachmentSchema;
  private final ObjectMapper objectMapper;

  public JsonSchemaValidator(ObjectMapper objectMapper) throws IOException {
    this.objectMapper = objectMapper;

    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    // Load schemas from resources
    this.archiveSchema =
        factory.getSchema(loadSchemaFromResource("json-schemas/chat-note-schema.json"));
    this.metadataSchema =
        factory.getSchema(loadSchemaFromResource("json-schemas/metadata-schema.json"));
    this.artifactSchema =
        factory.getSchema(loadSchemaFromResource("json-schemas/artifact-schema.json"));
    this.attachmentSchema =
        factory.getSchema(loadSchemaFromResource("json-schemas/attachment-schema.json"));

    log.info("JSON schemas loaded successfully");
  }

  public ValidationResult validate(String json) {
    try {
      JsonNode jsonNode = objectMapper.readTree(json);
      Set<ValidationMessage> errors = archiveSchema.validate(jsonNode);

      if (errors.isEmpty()) {
        return ValidationResult.builder().valid(true).errors(Collections.emptyList()).build();
      } else {
        List<String> errorMessages =
            errors.stream().map(ValidationMessage::getMessage).collect(Collectors.toList());

        return ValidationResult.builder().valid(false).errors(errorMessages).build();
      }
    } catch (Exception e) {
      log.error("Error validating JSON", e);
      return ValidationResult.builder().valid(false)
          .errors(Collections.singletonList(e.getMessage())).build();
    }
  }

  public ValidationResult validateMetadata(String json) {
    return validateAgainstSchema(json, metadataSchema);
  }

  public ValidationResult validateArtifact(String json) {
    return validateAgainstSchema(json, artifactSchema);
  }

  public ValidationResult validateAttachment(String json) {
    return validateAgainstSchema(json, attachmentSchema);
  }

  private ValidationResult validateAgainstSchema(String json, JsonSchema schema) {
    try {
      JsonNode jsonNode = objectMapper.readTree(json);
      Set<ValidationMessage> errors = schema.validate(jsonNode);

      if (errors.isEmpty()) {
        return ValidationResult.builder().valid(true).errors(Collections.emptyList()).build();
      } else {
        List<String> errorMessages =
            errors.stream().map(ValidationMessage::getMessage).collect(Collectors.toList());

        return ValidationResult.builder().valid(false).errors(errorMessages).build();
      }
    } catch (Exception e) {
      log.error("Error validating JSON against schema", e);
      return ValidationResult.builder().valid(false)
          .errors(Collections.singletonList(e.getMessage())).build();
    }
  }

  private InputStream loadSchemaFromResource(String path) throws IOException {
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
    if (inputStream == null) {
      throw new IOException("Schema file not found: " + path);
    }
    return inputStream;
  }

}
