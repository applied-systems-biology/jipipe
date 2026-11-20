package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.json.schema.JsonSchemaValidator;

import java.util.List;

/**
 * Validates operation parameters against simplified JSON schemas.
 * Supported keywords: type (object, string, integer, number, boolean, array), required, properties, enum,
 * and additionalProperties (false only). Unknown keywords are ignored.
 */
public class AutomationSchemaUtils {
    private AutomationSchemaUtils() {
    }

    /**
     * Parses a JSON literal into a node. Lets operations declare their schemas as readable literals.
     */
    public static JsonNode parseJson(String json) {
        try {
            return JsonUtils.getObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON literal: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the schema for operations that accept no parameters.
     */
    public static JsonNode emptyObjectSchema() {
        return parseJson("{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}");
    }

    /**
     * Validates arguments against a schema and returns the list of errors (empty when valid).
     * Null or missing arguments are treated as an empty object.
     * <p>
     * Fail-closed: a null or non-object schema (e.g. a malformed schema literal) produces a
     * single descriptive error instead of silently passing. The instance endpoint rejects
     * such operations before dispatch, so this is the second line of defense for direct
     * {@link AutomationOperationRegistry#execute} callers.
     */
    public static List<String> validate(JsonNode schema, JsonNode args) {
        return JsonSchemaValidator.validate(schema, args);
    }
}
