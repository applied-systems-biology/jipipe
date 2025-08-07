package edu.kit.datamanager.ro_crate.entities.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of the entity validation strategy that uses json schema.
 */
public class JsonSchemaValidation implements EntityValidationStrategy {

  private static final URL entitySchemaDefault
      = Objects.requireNonNull(JsonSchemaValidation.class.getClassLoader()
      .getResource("json_schemas/entity_schema.json"));
  private static final URL fieldSchemaDefault
      = Objects.requireNonNull(JsonSchemaValidation.class.getClassLoader()
      .getResource("json_schemas/entity_field_structure_schema.json"));

  private JsonSchema entitySchema;
  private JsonSchema entityFieldSchema;

  /**
   *  Default constructor that uses the default schemas.
   */
  public JsonSchemaValidation() {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    try {
      this.entitySchema = factory.getSchema(entitySchemaDefault.toURI());
      this.entityFieldSchema = factory.getSchema(fieldSchemaDefault.toURI());
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  /**
   * Constructor that uses custom schemas present as Json files.
   *
   * @param entitySchema schema for the entities validation.
   * @param fieldSchema schema for the field validation.
   */
  public JsonSchemaValidation(JsonNode entitySchema, JsonNode fieldSchema) {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    this.entitySchema = factory.getSchema(entitySchema);
    this.entityFieldSchema = factory.getSchema(fieldSchema);
  }

  @Override
  public boolean validateEntity(JsonNode entity) {
    Set<ValidationMessage> errors = this.entitySchema.validate(entity);
    if (errors.size() != 0) {
      System.err.println("This entity does not comply to the basic RO-Crate entity structure.");
      errors.forEach(error -> System.err.println(error.getMessage()));
      return false;
    }
    return true;
  }

  @Override
  public boolean validateFieldOfEntity(JsonNode field) {
    Set<ValidationMessage> errors = this.entityFieldSchema.validate(field);
    if (!errors.isEmpty()) {
      ObjectMapper objectMapper = MyObjectMapper.getMapper();
      System.err.println("The property: ");
      try {
        System.err.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(field));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
      System.err.println("does not comply with the flattened structure"
          + " of the RO-Crate json document.");
      return false;
    }
    return true;
  }
}
