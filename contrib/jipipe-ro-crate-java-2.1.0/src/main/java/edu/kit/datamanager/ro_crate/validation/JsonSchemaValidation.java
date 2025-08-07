package edu.kit.datamanager.ro_crate.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Set;

/**
 * Validation of the crate metadata using JSON-schema.
 */
public class JsonSchemaValidation implements ValidatorStrategy {

  private static final String defaultSchema = "json_schemas/default.json";
  private JsonSchema schema;

  private void getSchema(URI schemaUri) {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    this.schema = factory.getSchema(schemaUri);
  }

  /**
   * Default constructor for the JSON-schema validation.
   */
  public JsonSchemaValidation() {
    try {
      URI schemaUri = Objects.requireNonNull(
          getClass().getClassLoader().getResource(defaultSchema)).toURI();
      getSchema(schemaUri);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  public JsonSchemaValidation(URI schemaUri) {
    getSchema(schemaUri);
  }

  public JsonSchemaValidation(String schema) {
    URI schemaUri = new File(schema).toURI();
    getSchema(schemaUri);
  }

  public JsonSchemaValidation(JsonNode schema) {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    this.schema = factory.getSchema(schema);
  }

  @Override
  public boolean validate(Crate crate) {
    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    try {
      final JsonNode good = objectMapper.readTree(crate.getJsonMetadata());
      Set<ValidationMessage> errors = this.schema.validate(good);
      if (errors.size() == 0) {
        return true;
      } else {
        System.err.println("This crate does not validate against the this schema."
            + " If you haven't provided any schemas,"
            + " then it does not validate against the default one.");
        for (var e : errors) {
          System.err.println(e.getMessage());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }
}
